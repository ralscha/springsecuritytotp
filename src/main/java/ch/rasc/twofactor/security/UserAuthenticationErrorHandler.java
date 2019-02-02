package ch.rasc.twofactor.security;

import static ch.rasc.twofactor.db.Tables.APP_USER;

import java.time.LocalDateTime;

import org.jooq.DSLContext;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
public class UserAuthenticationErrorHandler
		implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

	private final DSLContext dsl;

	UserAuthenticationErrorHandler(DSLContext dsl) {
		this.dsl = dsl;
	}

	@Override
	public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
		Object principal = event.getAuthentication().getPrincipal();
		if (principal instanceof String) {
			String username = (String) principal;

			var result = this.dsl
					.select(APP_USER.ID, APP_USER.FAILED_LOGINS)
					.from(APP_USER)
					.where(APP_USER.USER_NAME.eq(username))
					.fetchOne();

			if (result != null) {
				Long userId = result.get(APP_USER.ID);

				Integer failedLogins = result.get(APP_USER.FAILED_LOGINS);
				if (failedLogins == null) {
					failedLogins = 1;
				}
				else {
					failedLogins++;
				}

				LocalDateTime lockedOut = null;
				if (failedLogins > 10) {
					lockedOut = LocalDateTime.now().plusMinutes(30);
				}

				this.dsl.update(APP_USER)
				        .set(APP_USER.LOCKED_OUT, lockedOut)
						.set(APP_USER.FAILED_LOGINS, failedLogins)
						.where(APP_USER.ID.eq(userId))
						.execute();
			}
			else {
				LoggerFactory.getLogger(UserAuthenticationErrorHandler.class)
						.error("Unknown user login attempt: {}", principal);
			}

		}
	}
}