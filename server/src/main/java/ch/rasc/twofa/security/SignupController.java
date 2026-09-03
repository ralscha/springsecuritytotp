package ch.rasc.twofa.security;

import static ch.rasc.twofa.db.tables.AppUser.APP_USER;

import java.util.Locale;

import org.jooq.DSLContext;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.passpol.PasswordPolicy;
import com.codahale.passpol.Status;

import ch.rasc.twofa.db.tables.records.AppUserRecord;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Validated
@RestController
public class SignupController {

	private final PasswordEncoder passwordEncoder;

	private final DSLContext dsl;

	private final PasswordPolicy passwordPolicy;

	public SignupController(PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy, DSLContext dsl) {
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
		this.dsl = dsl;
	}

	@PostMapping("/signup")
	public SignupResponse signup(@RequestParam("username") @NotBlank @Size(max = 255) String username,
			@RequestParam("password") @NotBlank @Size(max = 256) String password, @RequestParam("totp") boolean totp,
			HttpSession httpSession) {

		String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);

		var existingUser = this.dsl.selectFrom(APP_USER)
			.where(APP_USER.USERNAME.equalIgnoreCase(normalizedUsername))
			.fetchAny();
		if (existingUser != null) {
			return resumeTotpSignup(existingUser, password, totp, httpSession);
		}

		Status status = this.passwordPolicy.check(password);
		if (status != Status.OK) {
			return new SignupResponse(SignupResponse.Status.WEAK_PASSWORD);
		}

		if (totp) {
			String secret = CustomTotp.randomSecret();

			try {
				Long appUserId = this.dsl
					.insertInto(APP_USER, APP_USER.USERNAME, APP_USER.PASSWORD_HASH, APP_USER.ENABLED, APP_USER.SECRET,
							APP_USER.ADDITIONAL_SECURITY)
					.values(normalizedUsername, this.passwordEncoder.encode(password), false, secret, false)
					.returningResult(APP_USER.ID)
					.fetchSingle(APP_USER.ID);
				httpSession.setAttribute(SessionKeys.PENDING_SIGNUP_USER_ID, appUserId);
				httpSession.removeAttribute(SessionKeys.PENDING_AUTHENTICATION);
			}
			catch (IntegrityConstraintViolationException ex) {
				return new SignupResponse(SignupResponse.Status.USERNAME_TAKEN);
			}
			return new SignupResponse(SignupResponse.Status.OK, normalizedUsername, secret);
		}

		try {
			this.dsl
				.insertInto(APP_USER, APP_USER.USERNAME, APP_USER.PASSWORD_HASH, APP_USER.ENABLED, APP_USER.SECRET,
						APP_USER.ADDITIONAL_SECURITY)
				.values(normalizedUsername, this.passwordEncoder.encode(password), true, null, false)
				.execute();
			httpSession.removeAttribute(SessionKeys.PENDING_AUTHENTICATION);
		}
		catch (IntegrityConstraintViolationException ex) {
			return new SignupResponse(SignupResponse.Status.USERNAME_TAKEN);
		}
		httpSession.removeAttribute(SessionKeys.PENDING_SIGNUP_USER_ID);
		return new SignupResponse(SignupResponse.Status.OK);
	}

	@GetMapping("/signup-pending")
	public ResponseEntity<SignupResponse> pendingSignup(HttpSession httpSession) {
		Long appUserId = getPendingSignupUserId(httpSession);
		if (appUserId != null) {
			var record = this.dsl.select(APP_USER.USERNAME, APP_USER.SECRET, APP_USER.ENABLED)
				.from(APP_USER)
				.where(APP_USER.ID.eq(appUserId))
				.fetchOne();
			if (record != null && Boolean.FALSE.equals(record.get(APP_USER.ENABLED))
					&& isNotBlank(record.get(APP_USER.SECRET))) {
				return ResponseEntity.ok(new SignupResponse(SignupResponse.Status.OK, record.get(APP_USER.USERNAME),
						record.get(APP_USER.SECRET)));
			}
		}

		httpSession.removeAttribute(SessionKeys.PENDING_SIGNUP_USER_ID);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/signup-confirm-secret")
	public boolean signupConfirmSecret(@RequestParam("code") @Pattern(regexp = "\\d{6}") String code,
			HttpSession httpSession) {

		Long appUserId = getPendingSignupUserId(httpSession);
		if (appUserId == null) {
			return false;
		}
		var record = this.dsl.select(APP_USER.ID, APP_USER.SECRET)
			.from(APP_USER)
			.where(APP_USER.ID.eq(appUserId).and(APP_USER.ENABLED.isFalse()))
			.fetchOne();
		if (record != null) {
			String secret = record.get(APP_USER.SECRET);
			CustomTotp.Result result = isNotBlank(secret) ? new CustomTotp(secret).verify(code, 1, 1) : null;
			if (result != null && result.isValid()) {
				int updated = this.dsl.update(APP_USER)
					.set(APP_USER.ENABLED, true)
					.set(APP_USER.LAST_TOTP_INTERVAL, result.getMatchedInterval())
					.where(APP_USER.ID.eq(record.get(APP_USER.ID)).and(APP_USER.ENABLED.isFalse()))
					.execute();
				if (updated == 1) {
					httpSession.removeAttribute(SessionKeys.PENDING_SIGNUP_USER_ID);
					return true;
				}
			}
		}
		else {
			httpSession.removeAttribute(SessionKeys.PENDING_SIGNUP_USER_ID);
		}

		return false;
	}

	private static boolean isNotBlank(String str) {
		return str != null && !str.isBlank();
	}

	private SignupResponse resumeTotpSignup(AppUserRecord existingUser, String password, boolean totp,
			HttpSession httpSession) {
		if (totp && Boolean.FALSE.equals(existingUser.getEnabled()) && isNotBlank(existingUser.getSecret())
				&& this.passwordEncoder.matches(password, existingUser.getPasswordHash())) {
			httpSession.setAttribute(SessionKeys.PENDING_SIGNUP_USER_ID, existingUser.getId());
			httpSession.removeAttribute(SessionKeys.PENDING_AUTHENTICATION);
			return new SignupResponse(SignupResponse.Status.OK, existingUser.getUsername(), existingUser.getSecret());
		}
		return new SignupResponse(SignupResponse.Status.USERNAME_TAKEN);
	}

	private static Long getPendingSignupUserId(HttpSession httpSession) {
		Object appUserId = httpSession.getAttribute(SessionKeys.PENDING_SIGNUP_USER_ID);
		return appUserId instanceof Long id ? id : null;
	}

}
