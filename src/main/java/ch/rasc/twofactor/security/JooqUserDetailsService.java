package ch.rasc.twofactor.security;

import static ch.rasc.twofactor.db.Tables.APP_ROLE;
import static ch.rasc.twofactor.db.Tables.APP_USER;
import static ch.rasc.twofactor.db.Tables.APP_USER_ROLES;

import java.util.List;

import org.jooq.DSLContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import ch.rasc.twofactor.db.tables.pojos.AppUser;
import ch.rasc.twofactor.db.tables.records.AppUserRecord;

@Component
public class JooqUserDetailsService implements UserDetailsService {

	private final DSLContext dsl;

	public JooqUserDetailsService(DSLContext dsl) {
		this.dsl = dsl;
	}

	@Override
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {

		AppUserRecord appUserRecord = this.dsl
				.selectFrom(APP_USER)
				.where(APP_USER.USER_NAME.eq(username))
				.limit(1)
				.fetchOne();

		if (appUserRecord != null) {
			AppUser appUser = appUserRecord.into(AppUser.class);

			List<String> roles = this.dsl
			    .select(APP_ROLE.NAME)
			    .from(APP_ROLE)
			    .join(APP_USER_ROLES)
			       .on(APP_ROLE.ID.eq(APP_USER_ROLES.APP_ROLE_ID))
			    .where(APP_USER_ROLES.APP_USER_ID.eq(appUser.getId()))
			    .fetch(APP_ROLE.NAME);

			return new JooqUserDetails(appUser, roles);
		}
		throw new UsernameNotFoundException(username);
	}

}
