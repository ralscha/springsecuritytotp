package db.migration;


import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.using;

import java.time.LocalDateTime;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

public class V0002__initial_import extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

	  try (DSLContext dsl = using(context.getConnection())) {
		  Map<String, Long> roleNameToId = dsl.selectFrom(table("APP_ROLE")).fetchMap(field("NAME", String.class), field("ID", Long.class));

		  PasswordEncoder pe = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		  dsl.insertInto(table("APP_USER"),
				  field("ID"),
				  field("EMAIL"),
				  field("ENABLED"),
				  field("FIRST_NAME"),
				  field("LOCALE"),
				  field("NAME"),
				  field("PASSWORD_HASH"),
				  field("SECRET"),
				  field("USER_NAME"),
		          field("EXPIRATION_DATE", LocalDateTime.class))
		  		  .values(1, "test@test.ch", true, "admin", "en", "admin", pe.encode("admin"), "W4AU5VIXXCPZ3S6T", "admin", null)
		  		  .values(2, "user@test.ch", true, "user", "de", "user", pe.encode("user"), "LRVLAZ4WVFOU3JBF", "user", LocalDateTime.now().plusYears(1))
		  		  .values(3, "lazy@test.ch", true, "lazy", "en", "lazy", pe.encode("lazy"), null, "lazy", LocalDateTime.now().plusYears(1))
		  		  .execute();

		  dsl.insertInto(table("APP_USER_ROLES"), field("APP_USER_ID"), field("APP_ROLE_ID"))
		          .values(1, roleNameToId.get("ADMIN"))
		          .values(2, roleNameToId.get("USER"))
		          .values(3, roleNameToId.get("USER"))
		          .execute();
      }

  }
}