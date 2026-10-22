package ch.rasc.twofa.security;

import java.time.Clock;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import com.codahale.passpol.BreachDatabase;
import com.codahale.passpol.PasswordPolicy;

import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfig {

	@Bean
	DefaultConfigurationCustomizer disableSensitiveSqlLogging() {
		return configuration -> configuration.settings().withExecuteLogging(false);
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	CookieCsrfTokenRepository csrfTokenRepository(
			@Value("${app.security.secure-cookies:false}") boolean secureCookies) {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> {
			cookie.sameSite("Strict");
			if (secureCookies) {
				cookie.secure(true);
			}
		});
		return repository;
	}

	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy(CookieCsrfTokenRepository csrfTokenRepository) {
		return new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(),
				new CsrfAuthenticationStrategy(csrfTokenRepository)));
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
	}

	@Bean
	PasswordPolicy passwordPolicy() {
		return new PasswordPolicy(BreachDatabase.top100K(), 15, 256);
	}

	@Bean
	DelegatingSecurityContextRepository delegatingSecurityContextRepository() {
		return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
				new HttpSessionSecurityContextRepository());
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
		http.csrf(customizer -> customizer.csrfTokenRepository(csrfTokenRepository)
			.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
			.securityContext(
					securityContext -> securityContext.securityContextRepository(delegatingSecurityContextRepository()))
			.requestCache(cache -> cache.disable())
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
			.headers(headers -> headers
				.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; "
						+ "style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; "
						+ "connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'"))
				.referrerPolicy(policy -> policy.policy(ReferrerPolicy.NO_REFERRER))
				.frameOptions(frame -> frame.deny()))
			.authorizeHttpRequests(customizer -> {
				customizer.dispatcherTypeMatchers(DispatcherType.ERROR)
					.permitAll()
					.requestMatchers("/authenticate", "/signin", "/verify-totp", "/verify-totp-additional-security",
							"/signup", "/signup-pending", "/signup-confirm-secret", "/csrf")
					.permitAll()
					.requestMatchers("/", "/assets/**", "/svg/**", "/*.br", "/*.gz", "/*.html", "/*.js", "/*.css",
							"/*.woff2", "/*.ttf", "/*.eot", "/*.svg", "/*.woff", "/*.ico")
					.permitAll() // Permit all for these resources
					.anyRequest()
					.authenticated();
			})
			.logout(customizer -> customizer.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));
		return http.build();
	}

}
