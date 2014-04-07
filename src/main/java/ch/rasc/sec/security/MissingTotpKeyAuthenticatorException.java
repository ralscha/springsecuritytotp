package ch.rasc.sec.security;

import org.springframework.security.core.AuthenticationException;

public class MissingTotpKeyAuthenticatorException extends AuthenticationException {

	private static final long serialVersionUID = 1L;

	public MissingTotpKeyAuthenticatorException(String msg) {
		super(msg);
	}

}
