package ch.rasc.twofa.security;

import static ch.rasc.twofa.db.tables.AppUser.APP_USER;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.apache.commons.codec.digest.DigestUtils;
import org.jooq.DSLContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.rasc.twofa.db.tables.records.AppUserRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
public class AuthController {

	private final PasswordEncoder passwordEncoder;

	private final DSLContext dsl;

	private final String userNotFoundEncodedPassword;

	private final SecurityContextRepository securityContextRepository;

	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

	private final LoginAttemptLimiter attemptLimiter;

	private final Clock clock;

	public AuthController(PasswordEncoder passwordEncoder, DSLContext dsl,
			SecurityContextRepository securityContextRepository,
			SessionAuthenticationStrategy sessionAuthenticationStrategy, LoginAttemptLimiter attemptLimiter,
			Clock clock) {
		this.passwordEncoder = passwordEncoder;
		this.dsl = dsl;
		this.securityContextRepository = securityContextRepository;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
		this.attemptLimiter = attemptLimiter;
		this.clock = clock;
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
			AppUserRecord pendingUser = getPendingUser(httpSession);
			if (pendingUser != null) {
				return Boolean.TRUE.equals(pendingUser.getAdditionalSecurity())
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
		httpSession.removeAttribute(SessionKeys.TOTP_SHIFT);
		SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
		SecurityContextHolder.setContext(emptyContext);
		this.securityContextRepository.saveContext(emptyContext, request, response);
		String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
		this.attemptLimiter.check("signin-ip:" + request.getRemoteAddr(), 30);
		this.attemptLimiter.check("password:" + normalizedUsername, 10);
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
					httpSession.setAttribute(SessionKeys.PENDING_AUTHENTICATION,
							new PendingAuthentication(appUserRecord.getId(), credentialsFingerprint(appUserRecord),
									this.clock.millis() + Duration.ofMinutes(5).toMillis()));

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
		AppUserRecord user = getPendingUser(httpSession);
		if (user == null) {
			return AuthenticationFlow.NOT_AUTHENTICATED;
		}

		this.attemptLimiter.check("totp:" + user.getId(), 5);
		if (Boolean.TRUE.equals(user.getAdditionalSecurity())) {
			return AuthenticationFlow.TOTP_ADDITIONAL_SECURITY;
		}

		String secret = user.getSecret();
		if (isNotBlank(secret)) {
			CustomTotp totp = new CustomTotp(secret, () -> this.clock.millis() / 30_000);
			CustomTotp.Result result = totp.verify(code, 1, 1);
			if (result.isValid()) {
				if (consumeTotpInterval(user, result.getMatchedInterval(), result.getMatchedInterval())) {
					httpSession.removeAttribute(SessionKeys.PENDING_AUTHENTICATION);
					authenticate(new AppUserAuthentication(new AppUserDetail(user)), request, response);
					return AuthenticationFlow.AUTHENTICATED;
				}
				return AuthenticationFlow.TOTP;
			}

			setAdditionalSecurityFlag(user.getId());
			return AuthenticationFlow.TOTP_ADDITIONAL_SECURITY;
		}

		return AuthenticationFlow.NOT_AUTHENTICATED;
	}

	@PostMapping("/verify-totp-additional-security")
	public AuthenticationFlow verifyTotpAdditionalSecurity(@RequestParam @Pattern(regexp = "\\d{6}") String code1,
			@RequestParam @Pattern(regexp = "\\d{6}") String code2,
			@RequestParam @Pattern(regexp = "\\d{6}") String code3, HttpSession httpSession, HttpServletRequest request,
			HttpServletResponse response) {

		AppUserRecord user = getPendingUser(httpSession);
		if (user == null) {
			return AuthenticationFlow.NOT_AUTHENTICATED;
		}
		this.attemptLimiter.check("totp:" + user.getId(), 5);
		if (!Boolean.TRUE.equals(user.getAdditionalSecurity())) {
			return AuthenticationFlow.TOTP;
		}

		String secret = user.getSecret();
		if (isNotBlank(secret)) {
			CustomTotp totp = new CustomTotp(secret, () -> this.clock.millis() / 30_000);

			// The entire sequence must fit within five minutes of the server clock.
			CustomTotp.Result result = totp.verify(List.of(code1, code2, code3), 10, 8);
			if (result.isValid()) {
				long lastCodeInterval = result.getMatchedInterval() + 2;
				if (!consumeTotpInterval(user, result.getMatchedInterval(), lastCodeInterval)) {
					return AuthenticationFlow.TOTP_ADDITIONAL_SECURITY;
				}
				if (result.getShift() > 2 || result.getShift() < -2) {
					httpSession.setAttribute(SessionKeys.TOTP_SHIFT, result.getShift());
				}

				httpSession.removeAttribute(SessionKeys.PENDING_AUTHENTICATION);

				authenticate(new AppUserAuthentication(new AppUserDetail(user)), request, response);
				return AuthenticationFlow.AUTHENTICATED;
			}
		}

		return AuthenticationFlow.TOTP_ADDITIONAL_SECURITY;
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

	private AppUserRecord getPendingUser(HttpSession httpSession) {
		Object authentication = httpSession.getAttribute(SessionKeys.PENDING_AUTHENTICATION);
		if (!(authentication instanceof PendingAuthentication pending) || this.clock.millis() >= pending.expiresAt()) {
			httpSession.removeAttribute(SessionKeys.PENDING_AUTHENTICATION);
			return null;
		}

		AppUserRecord currentUser = this.dsl.selectFrom(APP_USER)
			.where(APP_USER.ID.eq(pending.userId()).and(APP_USER.ENABLED.isTrue()))
			.fetchOne();
		if (currentUser == null || !isNotBlank(currentUser.getSecret())
				|| !credentialsFingerprint(currentUser).equals(pending.credentialsFingerprint())) {
			httpSession.removeAttribute(SessionKeys.PENDING_AUTHENTICATION);
			return null;
		}

		return currentUser;
	}

	private boolean consumeTotpInterval(AppUserRecord user, long firstInterval, long lastInterval) {
		return this.dsl.update(APP_USER)
			.set(APP_USER.LAST_TOTP_INTERVAL, lastInterval)
			.set(APP_USER.ADDITIONAL_SECURITY, false)
			.where(APP_USER.ID.eq(user.getId())
				.and(APP_USER.ENABLED.isTrue())
				.and(APP_USER.ADDITIONAL_SECURITY.eq(user.getAdditionalSecurity()))
				.and(APP_USER.SECRET.eq(user.getSecret()))
				.and(APP_USER.PASSWORD_HASH.eq(user.getPasswordHash()))
				.and(APP_USER.LAST_TOTP_INTERVAL.isNull().or(APP_USER.LAST_TOTP_INTERVAL.lt(firstInterval))))
			.execute() == 1;
	}

	private void authenticate(AppUserAuthentication authentication, HttpServletRequest request,
			HttpServletResponse response) {
		this.sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		this.securityContextRepository.saveContext(context, request, response);
	}

	private static String credentialsFingerprint(AppUserRecord user) {
		return DigestUtils.sha256Hex(user.getPasswordHash() + ":" + user.getSecret());
	}

	record PendingAuthentication(Long userId, String credentialsFingerprint, long expiresAt) implements Serializable {
	}

}
