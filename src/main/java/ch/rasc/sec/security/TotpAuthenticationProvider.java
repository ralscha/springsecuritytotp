package ch.rasc.sec.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

public class TotpAuthenticationProvider extends DaoAuthenticationProvider {

	@Override
	protected void additionalAuthenticationChecks(UserDetails userDetails,
			UsernamePasswordAuthenticationToken authentication)
			throws AuthenticationException {

		super.additionalAuthenticationChecks(userDetails, authentication);

		if (authentication.getDetails() instanceof TotpWebAuthenticationDetails) {
			String secret = ((JpaUserDetails) userDetails).getSecret();

			if (StringUtils.hasText(secret)) {
				Integer totpKey = ((TotpWebAuthenticationDetails) authentication
						.getDetails()).getTotpKey();
				if (totpKey != null) {
					try {
						if (!TotpAuthenticatorUtil.verifyCode(secret, totpKey, 2)) {
							throw new BadCredentialsException(
									"Google Authenticator Code is not valid");
						}
					}
					catch (InvalidKeyException | NoSuchAlgorithmException e) {
						throw new InternalAuthenticationServiceException(
								"Google Authenticator Code verify failed", e);
					}

				}
				else {
					throw new MissingTotpKeyAuthenticatorException(
							"Google Authenticator Code is mandatory");
				}

			}
		}

	}

}
