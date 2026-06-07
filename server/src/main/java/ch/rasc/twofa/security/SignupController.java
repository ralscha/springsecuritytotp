package ch.rasc.twofa.security;

import static ch.rasc.twofa.db.tables.AppUser.APP_USER;

import org.jooq.exception.DataAccessException;
import org.jooq.DSLContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.passpol.PasswordPolicy;
import com.codahale.passpol.Status;

import jakarta.validation.constraints.NotBlank;

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
	public SignupResponse signup(@RequestParam("username") @NotBlank String username,
			@RequestParam("password") @NotBlank String password, @RequestParam("totp") boolean totp) {

		String normalizedUsername = username.trim();

		// cancel if the user is already registered
		int count = this.dsl.selectCount()
			.from(APP_USER)
			.where(APP_USER.USERNAME.equalIgnoreCase(normalizedUsername))
			.fetchOne(0, int.class);
		if (count > 0) {
			return new SignupResponse(SignupResponse.Status.USERNAME_TAKEN);
		}

		Status status = this.passwordPolicy.check(password);
		if (status != Status.OK) {
			return new SignupResponse(SignupResponse.Status.WEAK_PASSWORD);
		}

		if (totp) {
			String secret = CustomTotp.randomSecret();

			try {
				this.dsl
					.insertInto(APP_USER, APP_USER.USERNAME, APP_USER.PASSWORD_HASH, APP_USER.ENABLED, APP_USER.SECRET,
							APP_USER.ADDITIONAL_SECURITY)
					.values(normalizedUsername, this.passwordEncoder.encode(password), false, secret, false)
					.execute();
			}
			catch (DataAccessException ex) {
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
		}
		catch (DataAccessException ex) {
			return new SignupResponse(SignupResponse.Status.USERNAME_TAKEN);
		}
		return new SignupResponse(SignupResponse.Status.OK);
	}

	@PostMapping("/signup-confirm-secret")
	public boolean signupConfirmSecret(@RequestParam("username") @NotBlank String username,
			@RequestParam("code") @NotBlank String code) {

		var record = this.dsl.select(APP_USER.ID, APP_USER.SECRET)
			.from(APP_USER)
			.where(APP_USER.USERNAME.eq(username.trim()))
			.fetchOne();
		if (record != null) {
			String secret = record.get(APP_USER.SECRET);
			if (isNotBlank(secret) && new CustomTotp(secret).verify(code, 2, 2).isValid()) {
				this.dsl.update(APP_USER)
					.set(APP_USER.ENABLED, true)
					.where(APP_USER.ID.eq(record.get(APP_USER.ID)))
					.execute();
				return true;
			}
		}

		return false;
	}

	private static boolean isNotBlank(String str) {
		return str != null && !str.isBlank();
	}

}
