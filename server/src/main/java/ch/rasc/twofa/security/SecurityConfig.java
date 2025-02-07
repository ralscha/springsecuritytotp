package ch.rasc.twofa.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

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
  public PasswordEncoder passwordEncoder() {
    return new Argon2PasswordEncoder(16, 32, 8, 1 << 16, 4);
  }

  @Bean
  public PasswordPolicy passwordPolicy() {
    return new PasswordPolicy(BreachDatabase.top100K(), 8, 256);
  }

  @Scope("prototype")
  @Bean
  MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
    return new MvcRequestMatcher.Builder(introspector);
  }

  @Bean
  public DelegatingSecurityContextRepository delegatingSecurityContextRepository() {
    return new DelegatingSecurityContextRepository(
        new RequestAttributeSecurityContextRepository(),
        new HttpSessionSecurityContextRepository());
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc)
      throws Exception {
    http.csrf(CsrfConfigurer::disable)
        .securityContext(securityContext -> securityContext
            .securityContextRepository(delegatingSecurityContextRepository()))
        .authorizeHttpRequests(customizer -> {
          customizer
              .requestMatchers(mvc.pattern("/authenticate"), mvc.pattern("/signin"),
                  mvc.pattern("/verify-totp"),
                  mvc.pattern("/verify-totp-additional-security"), mvc.pattern("/signup"),
                  mvc.pattern("/signup-confirm-secret"))
              .permitAll()
              .requestMatchers(mvc.pattern("/"), mvc.pattern("/assets/**"),
                  mvc.pattern("/svg/**"), mvc.pattern("/*.br"), mvc.pattern("/*.gz"),
                  mvc.pattern("/*.html"), mvc.pattern("/*.js"), mvc.pattern("/*.css"),
                  mvc.pattern("/*.woff2"), mvc.pattern("/*.ttf"), mvc.pattern("/*.eot"),
                  mvc.pattern("/*.svg"), mvc.pattern("/*.woff"), mvc.pattern("/*.ico")) // Add
                                                                                        // these
              .permitAll() // Permit all for these resources
              .anyRequest().authenticated();
        }).logout(customizer -> customizer
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));
    return http.build();
  }

}
