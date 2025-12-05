package ch.rasc.twofa.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

import com.codahale.passpol.BreachDatabase;
import com.codahale.passpol.PasswordPolicy;

@Configuration
public class SecurityConfig {

  @Bean
  AuthenticationManager authenticationManager() {
    return authentication -> {
      throw new AuthenticationServiceException("Cannot authenticate " + authentication);
    };
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new Argon2PasswordEncoder(16, 32, 8, 1 << 18, 4);
  }

  @Bean
  PasswordPolicy passwordPolicy() {
    return new PasswordPolicy(BreachDatabase.top100K(), 8, 256);
  }

  @Bean
  DelegatingSecurityContextRepository delegatingSecurityContextRepository() {
    return new DelegatingSecurityContextRepository(
        new RequestAttributeSecurityContextRepository(),
        new HttpSessionSecurityContextRepository());
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(CsrfConfigurer::disable)
        .securityContext(securityContext -> securityContext
            .securityContextRepository(delegatingSecurityContextRepository()))
        .authorizeHttpRequests(customizer -> {
          customizer
              .requestMatchers("/authenticate", "/signin", "/verify-totp",
                  "/verify-totp-additional-security", "/signup", "/signup-confirm-secret")
              .permitAll()
              .requestMatchers("/", "/assets/**", "/svg/**", "/*.br", "/*.gz", "/*.html",
                  "/*.js", "/*.css", "/*.woff2", "/*.ttf", "/*.eot", "/*.svg", "/*.woff",
                  "/*.ico")
              .permitAll() // Permit all for these resources
              .anyRequest().authenticated();
        }).logout(customizer -> customizer
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));
    return http.build();
  }

}
