package ch.rasc.twofactor.security;

import static ch.rasc.twofactor.db.Tables.APP_USER;

import java.util.Map;

import org.jooq.DSLContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LoginController {
	private final DSLContext dsl;

	LoginController(DSLContext dsl) {
		this.dsl = dsl;
	}

	@RequestMapping("/login")
	public String home(Map<String, Object> model) {
		addModelData("admin", model);
		addModelData("user", model);
		addModelData("lazy", model);
		return "login";
	}

	private void addModelData(String username, Map<String, Object> model) {
		var result = this.dsl
				.select(APP_USER.FAILED_LOGINS, APP_USER.LOCKED_OUT,
						APP_USER.EXPIRATION_DATE)
				.from(APP_USER).where(APP_USER.USER_NAME.eq(username)).limit(1)
				.fetchOne();

		if (result != null) {
			model.put(username + "FailedLogins", result.get(APP_USER.FAILED_LOGINS));
			model.put(username + "LockedOut", result.get(APP_USER.LOCKED_OUT));
			model.put(username + "ExpirationDate", result.get(APP_USER.EXPIRATION_DATE));
		}
	}
}
