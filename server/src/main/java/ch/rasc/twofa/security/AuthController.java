package ch.rasc.twofa.security;

import static ch.rasc.twofa.db.tables.AppUser.APP_USER;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jooq.DSLContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import ch.rasc.twofa.db.tables.records.AppUserRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Validated
@RestController
public class AuthController {

	private final PasswordEncoder passwordEncoder;

	private final DSLContext dsl;

	private final String userNotFoundEncodedPassword;

	private final SecurityContextRepository securityContextRepository;

	public AuthController(PasswordEncoder passwordEncoder, DSLContext dsl,
			SecurityContextRepository securityContextRepository) {
		this.passwordEncoder = passwordEncoder;
		this.dsl = dsl;
		this.securityContextRepository = securityContextRepository;
		this.userNotFoundEncodedPassword = this.passwordEncoder.encode("userNotFoundPassword");
	}

	@GetMapping("/authenticate")
	public AuthenticationFlow authenticate(HttpServletRequest request, CsrfToken csrfToken) {
		csrfToken.getToken();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth instanceof AppUserAuthentication) {
			return AuthenticationFlow.AUTHENTICATED;
		}

		HttpSession httpSession = request.getSession(false);
		if (httpSession != null) {
			AppUserAuthentication pendingAuthentication = getPendingAuthentication(httpSession);
			if (pendingAuthentication != null) {
				AppUserDetail detail = (AppUserDetail) pendingAuthentication.getPrincipal();
				return isUserInAdditionalSecurityMode(detail.getAppUserId())
						? AuthenticationFlow.TOTP_ADDITIONAL_SECURITY : AuthenticationFlow.TOTP;
			}
		}

		return AuthenticationFlow.NOT_AUTHENTICATED;
	}

	@GetMapping("/csrf")
	public void csrf(CsrfToken csrfToken) {
		csrfToken.getToken();
	}

	@PostMapping("/signin")
	public AuthenticationFlow login(@RequestParam @NotBlank @Size(max = 255) String username,
			@RequestParam @NotBlank @Size(max = 256) String password, HttpSession httpSession,
			HttpServletRequest request, HttpServletResponse response) {

		httpSession.removeAttribute(SessionKeys.PENDING_AUTHENTICATION);
		String normalizedUsername = username.trim();
		AppUserRecord appUserRecord = this.dsl.selectFrom(APP_USER)
			.where(APP_USER.USERNAME.equalIgnoreCase(normalizedUsername))
			.fetchAny();

		if (appUserRecord != null) {
			String passwordHash = appUserRecord.getPasswordHash();
			boolean pwMatches = passwordHash != null && this.passwordEncoder.matches(password, passwordHash);
			if (pwMatches && Boolean.TRUE.equals(appUserRecord.getEnabled())) {
				request.changeSessionId();
				httpSession.removeAttribute(SessionKeys.PENDING_SIGNUP_USER_ID);

				AppUserDetail detail = new AppUserDetail(appUserRecord);
				AppUserAuthentication userAuthentication = new AppUserAuthentication(detail);
				if (isNotBlank(appUserRecord.getSecret())) {
					httpSession.setAttribute(SessionKeys.PENDING_AUTHENTICATION, userAuthentication);

					if (isUserInAdditionalSecurityMode(detail.getAppUserId())) {
						return AuthenticationFlow.TOTP_ADDITIONAL_SECURITY;
					}

					return AuthenticationFlow.TOTP;
				}

				authenticate(userAuthentication, request, response);
				return AuthenticationFlow.AUTHENTICATED;
			}
		}
		else {
			this.passwordEncoder.matches(password, this.userNotFoundEncodedPassword);
		}

		return AuthenticationFlow.NOT_AUTHENTICATED;
	}

	@PostMapping("/verify-totp")
	public AuthenticationFlow totp(@RequestParam @Pattern(regexp = "\\d{6}") String code, HttpSession httpSession,
			HttpServletRequest request, HttpServletResponse response) {
		AppUserAuthentication userAuthentication = getPendingAuthentication(httpSession);
		if (userAuthentication == null) {
			return AuthenticationFlow.NOT_AUTHENTICATED;
		}

		AppUserDetail detail = (AppUserDetail) userAuthentication.getPrincipal();
		if (isUserInAdditionalSecurityMode(detail.getAppUserId())) {
			return AuthenticationFlow.TOTP_ADDITIONAL_SECURITY;
		}

		String secret = detail.getSecret();
		if (isNotBlank(secret)) {
			CustomTotp totp = new CustomTotp(secret);
			CustomTotp.Result result = totp.verify(code, 1, 1);
			if (result.isValid()) {
				if (consumeTotpInterval(detail.getAppUserId(), result.getMatchedInterval())) {
					httpSession.removeAttribute(SessionKeys.PENDING_AUTHENTICATION);
					authenticate(userAuthentication, request, response);
					return AuthenticationFlow.AUTHENTICATED;
				}
				return AuthenticationFlow.TOTP;
			}

			setAdditionalSecurityFlag(detail.getAppUserId());
			return AuthenticationFlow.TOTP_ADDITIONAL_SECURITY;
		}

		return AuthenticationFlow.NOT_AUTHENTICATED;
	}

	@PostMapping("/verify-totp-additional-security")
	public AuthenticationFlow verifyTotpAdditionalSecurity(@RequestParam @Pattern(regexp = "\\d{6}") String code1,
			@RequestParam @Pattern(regexp = "\\d{6}") String code2,
			@RequestParam @Pattern(regexp = "\\d{6}") String code3, HttpSession httpSession, HttpServletRequest request,
			HttpServletResponse response) {

		AppUserAuthentication userAuthentication = getPendingAuthentication(httpSession);
		if (userAuthentication == null || code1.equals(code2) || code1.equals(code3) || code2.equals(code3)) {
			return AuthenticationFlow.NOT_AUTHENTICATED;
		}

		AppUserDetail detail = (AppUserDetail) userAuthentication.getPrincipal();
		String secret = detail.getSecret();
		if (isNotBlank(secret)) {
			CustomTotp totp = new CustomTotp(secret);

			// check 25 hours into the past and future.
			long noOf30SecondsIntervals = TimeUnit.HOURS.toSeconds(25) / 30;
			CustomTotp.Result result = totp.verify(List.of(code1, code2, code3), noOf30SecondsIntervals,
					noOf30SecondsIntervals);
			if (result.isValid()) {
				if (result.getShift() > 2 || result.getShift() < -2) {
					httpSession.setAttribute(SessionKeys.TOTP_SHIFT, result.getShift());
				}

				long lastCodeInterval = result.getMatchedInterval() + 2;
				if (!consumeTotpInterval(detail.getAppUserId(), lastCodeInterval)) {
					return AuthenticationFlow.NOT_AUTHENTICATED;
				}
				clearAdditionalSecurityFlag(detail.getAppUserId());
				httpSession.removeAttribute(SessionKeys.PENDING_AUTHENTICATION);

				authenticate(userAuthentication, request, response);
				return AuthenticationFlow.AUTHENTICATED;
			}
		}

		return AuthenticationFlow.NOT_AUTHENTICATED;
	}

	@GetMapping("/totp-shift")
	public String getTotpShift(HttpSession httpSession) {
		Long shift = (Long) httpSession.getAttribute(SessionKeys.TOTP_SHIFT);
		if (shift == null) {
			return null;
		}
		httpSession.removeAttribute(SessionKeys.TOTP_SHIFT);

		StringBuilder out = new StringBuilder();
		long total30Seconds = Math.abs(shift);
		long hours = total30Seconds / 120;
		total30Seconds = total30Seconds % 120;
		long minutes = total30Seconds / 2;
		boolean seconds = total30Seconds % 2 != 0;

		if (hours == 1) {
			out.append("1 hour ");
		}
		else if (hours > 1) {
			out.append(hours).append(" hours ");
		}

		if (minutes == 1) {
			out.append("1 minute ");
		}
		else if (minutes > 1) {
			out.append(minutes).append(" minutes ");
		}

		if (seconds) {
			out.append("30 seconds ");
		}

		return out.append(shift < 0 ? "behind" : "ahead").toString();
	}

	private static boolean isNotBlank(String str) {
		return str != null && !str.isBlank();
	}

	private boolean isUserInAdditionalSecurityMode(long appUserId) {
		Boolean additionalSecurity = this.dsl.select(APP_USER.ADDITIONAL_SECURITY)
			.from(APP_USER)
			.where(APP_USER.ID.eq(appUserId))
			.fetchOne(APP_USER.ADDITIONAL_SECURITY);
		return Boolean.TRUE.equals(additionalSecurity);
	}

	private void setAdditionalSecurityFlag(Long appUserId) {
		this.dsl.update(APP_USER).set(APP_USER.ADDITIONAL_SECURITY, true).where(APP_USER.ID.eq(appUserId)).execute();
	}

	private void clearAdditionalSecurityFlag(Long appUserId) {
		this.dsl.update(APP_USER).set(APP_USER.ADDITIONAL_SECURITY, false).where(APP_USER.ID.eq(appUserId)).execute();
	}

	private AppUserAuthentication getPendingAuthentication(HttpSession httpSession) {
		Object authentication = httpSession.getAttribute(SessionKeys.PENDING_AUTHENTICATION);
		if (!(authentication instanceof AppUserAuthentication appUserAuthentication)) {
			return null;
		}

		AppUserDetail detail = (AppUserDetail) appUserAuthentication.getPrincipal();
		AppUserRecord currentUser = this.dsl.selectFrom(APP_USER)
			.where(APP_USER.ID.eq(detail.getAppUserId()).and(APP_USER.ENABLED.isTrue()))
			.fetchOne();
		if (currentUser == null || !isNotBlank(currentUser.getSecret())) {
			httpSession.removeAttribute(SessionKeys.PENDING_AUTHENTICATION);
			return null;
		}

		AppUserAuthentication currentAuthentication = new AppUserAuthentication(new AppUserDetail(currentUser));
		httpSession.setAttribute(SessionKeys.PENDING_AUTHENTICATION, currentAuthentication);
		return currentAuthentication;
	}

	private boolean consumeTotpInterval(Long appUserId, long interval) {
		return this.dsl.update(APP_USER)
			.set(APP_USER.LAST_TOTP_INTERVAL, interval)
			.where(APP_USER.ID.eq(appUserId)
				.and(APP_USER.LAST_TOTP_INTERVAL.isNull().or(APP_USER.LAST_TOTP_INTERVAL.ne(interval))))
			.execute() == 1;
	}

	private void authenticate(AppUserAuthentication authentication, HttpServletRequest request,
			HttpServletResponse response) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		this.securityContextRepository.saveContext(context, request, response);
	}

}
