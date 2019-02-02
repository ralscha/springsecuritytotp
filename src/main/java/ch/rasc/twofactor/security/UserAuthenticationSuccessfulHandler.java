package ch.rasc.twofactor.security;

import static ch.rasc.twofactor.db.Tables.APP_USER;

import java.time.LocalDateTime;

import org.jooq.DSLContext;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class UserAuthenticationSuccessfulHandler
		implements ApplicationListener<InteractiveAuthenticationSuccessEvent> {

	private final DSLContext dsl;

	UserAuthenticationSuccessfulHandler(DSLContext dsl) {
		this.dsl = dsl;
	}

	@Override
	public void onApplicationEvent(InteractiveAuthenticationSuccessEvent event) {
		Object principal = event.getAuthentication().getPrincipal();
		if (principal instanceof JooqUserDetails) {
			this.dsl.update(APP_USER)
			        .set(APP_USER.LOCKED_OUT, (LocalDateTime) null)
					.set(APP_USER.FAILED_LOGINS, (Integer) null)
					.set(APP_USER.EXPIRATION_DATE, LocalDateTime.now().plusYears(1))
					.where(APP_USER.ID.eq(((JooqUserDetails) principal).getUserDbId()))
					.execute();
		}
	}
}