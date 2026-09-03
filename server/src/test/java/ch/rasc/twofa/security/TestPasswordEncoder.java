package ch.rasc.twofa.security;

import org.springframework.security.crypto.password.PasswordEncoder;

final class TestPasswordEncoder implements PasswordEncoder {

	@Override
	public String encode(CharSequence rawPassword) {
		return "test:" + rawPassword;
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		return encode(rawPassword).equals(encodedPassword);
	}

}
