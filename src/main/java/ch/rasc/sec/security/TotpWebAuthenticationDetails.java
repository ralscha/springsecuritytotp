package ch.rasc.sec.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.util.StringUtils;

public class TotpWebAuthenticationDetails extends WebAuthenticationDetails {

	private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

	private Integer totpKey;

	public TotpWebAuthenticationDetails(HttpServletRequest request) {
		super(request);

		String totpKeyString = request.getParameter("totpkey");
		if (StringUtils.hasText(totpKeyString)) {
			try {
				this.totpKey = Integer.valueOf(totpKeyString);
			}
			catch (NumberFormatException e) {
				this.totpKey = null;
			}
		}
	}

	public Integer getTotpKey() {
		return this.totpKey;
	}

}
