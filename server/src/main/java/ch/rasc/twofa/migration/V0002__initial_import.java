package ch.rasc.twofa.migration;

import static ch.rasc.twofa.db.tables.AppUser.APP_USER;
import static org.jooq.impl.DSL.using;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class V0002__initial_import extends BaseJavaMigration {

  private final PasswordEncoder passwordEncoder;

  public V0002__initial_import(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void migrate(Context context) throws Exception {
    @SuppressWarnings("resource")
    DSLContext dsl = using(context.getConnection());
    dsl.insertInto(APP_USER, APP_USER.USERNAME, APP_USER.PASSWORD_HASH, APP_USER.SECRET,
        APP_USER.ENABLED, APP_USER.ADDITIONAL_SECURITY)
        .values("admin", this.passwordEncoder.encode("admin"), "W4AU5VIXXCPZ3S6T", true,
            false)
        .values("user", this.passwordEncoder.encode("user"), "LRVLAZ4WVFOU3JBF", true,
            false)
        .values("lazy", this.passwordEncoder.encode("lazy"), null, true, false).execute();
  }
}