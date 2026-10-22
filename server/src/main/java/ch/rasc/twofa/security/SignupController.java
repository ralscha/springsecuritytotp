package ch.rasc.twofa.security;

import static ch.rasc.twofa.db.tables.AppUser.APP_USER;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.passpol.PasswordPolicy;
import com.codahale.passpol.Status;

import ch.rasc.twofa.db.tables.records.AppUserRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
public class SignupController {

	static final Field<Boolean> ENROLLMENT_PENDING = field(name("ENROLLMENT_PENDING"), Boolean.class);

	static final Field<Long> ENROLLMENT_EXPIRES_AT = field(name("ENROLLMENT_EXPIRES_AT"), Long.class);

	private static final long ENROLLMENT_LIFETIME_MILLIS = TimeUnit.MINUTES.toMillis(10);

	private final PasswordEncoder passwordEncoder;

	private final DSLContext dsl;

	private final PasswordPolicy passwordPolicy;

	private final LoginAttemptLimiter attemptLimiter;

	public SignupController(PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy, DSLContext dsl,
			LoginAttemptLimiter attemptLimiter) {
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
		this.dsl = dsl;
		this.attemptLimiter = attemptLimiter;
	}

	@PostMapping("/signup")
	public SignupResponse signup(@RequestParam("username") @NotBlank @Size(max = 255) String username,
			@RequestParam("password") @NotBlank @Size(max = 256) String password, @RequestParam("totp") boolean totp,
			HttpSession httpSession, HttpServletRequest request) {

		this.attemptLimiter.check("signup:" + request.getRemoteAddr(), 10);
		httpSession.removeAttribute(SessionKeys.PENDING_SIGNUP_USER_ID);
		httpSession.removeAttribute(SessionKeys.PENDING_AUTHENTICATION);
		String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
		long now = System.currentTimeMillis();
		// Expired, unfinished registrations no longer reserve a username.
		this.dsl.deleteFrom(APP_USER)
			.where(APP_USER.USERNAME.equalIgnoreCase(normalizedUsername)
				.and(APP_USER.ENABLED.isFalse())
				.and(ENROLLMENT_PENDING.isTrue())
				.and(ENROLLMENT_EXPIRES_AT.le(now)))
			.execute();

		var existingUser = this.dsl.selectFrom(APP_USER)
			.where(APP_USER.USERNAME.equalIgnoreCase(normalizedUsername))
			.fetchAny();
		if (existingUser != null) {
			return resumeTotpSignup(existingUser, password, totp, httpSession, request);
		}

		Status status = this.passwordPolicy.check(password);
		if (status != Status.OK) {
			return new SignupResponse(SignupResponse.Status.WEAK_PASSWORD);
		}

		if (totp) {
			String secret = CustomTotp.randomSecret();
			long expiresAt = now + ENROLLMENT_LIFETIME_MILLIS;

			try {
				Long appUserId = this.dsl
					.insertInto(APP_USER, APP_USER.USERNAME, APP_USER.PASSWORD_HASH, APP_USER.ENABLED, APP_USER.SECRET,
							APP_USER.ADDITIONAL_SECURITY, ENROLLMENT_PENDING, ENROLLMENT_EXPIRES_AT)
					.values(normalizedUsername, this.passwordEncoder.encode(password), false, secret, false, true,
							expiresAt)
					.returningResult(APP_USER.ID)
					.fetchSingle(APP_USER.ID);
				request.changeSessionId();
				httpSession.setAttribute(SessionKeys.PENDING_SIGNUP_USER_ID, new PendingSignup(appUserId, expiresAt));
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
			request.changeSessionId();
		}
		catch (IntegrityConstraintViolationException ex) {
			return new SignupResponse(SignupResponse.Status.USERNAME_TAKEN);
		}
		httpSession.removeAttribute(SessionKeys.PENDING_SIGNUP_USER_ID);
		return new SignupResponse(SignupResponse.Status.OK);
	}

	@GetMapping("/signup-pending")
	public ResponseEntity<SignupResponse> pendingSignup(HttpSession httpSession) {
		PendingSignup pending = getPendingSignup(httpSession);
		if (pending != null) {
			var record = this.dsl.select(APP_USER.USERNAME, APP_USER.SECRET, APP_USER.ENABLED)
				.from(APP_USER)
				.where(APP_USER.ID.eq(pending.appUserId())
					.and(ENROLLMENT_PENDING.isTrue())
					.and(ENROLLMENT_EXPIRES_AT.eq(pending.expiresAt()))
					.and(ENROLLMENT_EXPIRES_AT.gt(System.currentTimeMillis())))
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

		PendingSignup pending = getPendingSignup(httpSession);
		if (pending == null) {
			return false;
		}
		this.attemptLimiter.check("enrollment:" + pending.appUserId(), 5);
		var record = this.dsl.select(APP_USER.ID, APP_USER.SECRET)
			.from(APP_USER)
			.where(APP_USER.ID.eq(pending.appUserId())
				.and(APP_USER.ENABLED.isFalse())
				.and(ENROLLMENT_PENDING.isTrue())
				.and(ENROLLMENT_EXPIRES_AT.eq(pending.expiresAt()))
				.and(ENROLLMENT_EXPIRES_AT.gt(System.currentTimeMillis())))
			.fetchOne();
		if (record != null) {
			String secret = record.get(APP_USER.SECRET);
			CustomTotp.Result result = isNotBlank(secret) ? new CustomTotp(secret).verify(code, 1, 1) : null;
			if (result != null && result.isValid()) {
				int updated = this.dsl.update(APP_USER)
					.set(APP_USER.ENABLED, true)
					.set(ENROLLMENT_PENDING, false)
					.setNull(ENROLLMENT_EXPIRES_AT)
					.set(APP_USER.LAST_TOTP_INTERVAL, result.getMatchedInterval())
					.where(APP_USER.ID.eq(record.get(APP_USER.ID))
						.and(APP_USER.ENABLED.isFalse())
						.and(ENROLLMENT_PENDING.isTrue())
						.and(ENROLLMENT_EXPIRES_AT.eq(pending.expiresAt()))
						.and(ENROLLMENT_EXPIRES_AT.gt(System.currentTimeMillis())))
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
			HttpSession httpSession, HttpServletRequest request) {
		if (totp && Boolean.FALSE.equals(existingUser.getEnabled()) && isNotBlank(existingUser.getSecret())) {
			this.attemptLimiter.check("password:" + existingUser.getUsername().toLowerCase(Locale.ROOT), 10);
			if (existingUser.getPasswordHash() == null
					|| !this.passwordEncoder.matches(password, existingUser.getPasswordHash())) {
				return new SignupResponse(SignupResponse.Status.USERNAME_TAKEN);
			}
			// A disabled account is not necessarily an unfinished enrollment. Recheck
			// its explicit enrollment state after password verification.
			var pending = this.dsl.select(APP_USER.USERNAME, APP_USER.SECRET, ENROLLMENT_EXPIRES_AT)
				.from(APP_USER)
				.where(APP_USER.ID.eq(existingUser.getId())
					.and(APP_USER.ENABLED.isFalse())
					.and(ENROLLMENT_PENDING.isTrue())
					.and(ENROLLMENT_EXPIRES_AT.gt(System.currentTimeMillis())))
				.fetchOne();
			if (pending != null) {
				request.changeSessionId();
				httpSession.setAttribute(SessionKeys.PENDING_SIGNUP_USER_ID,
						new PendingSignup(existingUser.getId(), pending.get(ENROLLMENT_EXPIRES_AT)));
				return new SignupResponse(SignupResponse.Status.OK, pending.get(APP_USER.USERNAME),
						pending.get(APP_USER.SECRET));
			}
		}
		return new SignupResponse(SignupResponse.Status.USERNAME_TAKEN);
	}

	private static PendingSignup getPendingSignup(HttpSession httpSession) {
		Object pending = httpSession.getAttribute(SessionKeys.PENDING_SIGNUP_USER_ID);
		if (pending instanceof PendingSignup signup && signup.expiresAt() > System.currentTimeMillis()) {
			return signup;
		}
		httpSession.removeAttribute(SessionKeys.PENDING_SIGNUP_USER_ID);
		return null;
	}

	private record PendingSignup(Long appUserId, long expiresAt) implements Serializable {
	}

}
