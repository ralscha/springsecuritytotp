package ch.rasc.sec.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class TotpWebAuthenticationDetails extends WebAuthenticationDetails {

	private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

	private Integer totpKey;

	public TotpWebAuthenticationDetails(HttpServletRequest request) {
		super(request);

		String totpKeyString = request.getParameter("totpkey");
		try {
			totpKey = Integer.valueOf(totpKeyString);
		} catch (NumberFormatException e) {
			totpKey = null;
		}
	}

	public Integer getTotpKey() {
		return totpKey;
	}

}
