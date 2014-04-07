package ch.rasc.sec.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AuthenticationDetailsSource;

public class TotpWebAuthenticationDetailsSource implements
		AuthenticationDetailsSource<HttpServletRequest, TotpWebAuthenticationDetails> {

	@Override
	public TotpWebAuthenticationDetails buildDetails(HttpServletRequest request) {
		return new TotpWebAuthenticationDetails(request);
	}

}
