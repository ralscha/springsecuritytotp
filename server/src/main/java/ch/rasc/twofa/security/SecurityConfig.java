package ch.rasc.twofa.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.codahale.passpol.BreachDatabase;
import com.codahale.passpol.PasswordPolicy;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Bean
	@Override
	protected AuthenticationManager authenticationManager() throws Exception {
		return authentication -> {
			throw new AuthenticationServiceException(
					"Cannot authenticate " + authentication);
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

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:1234"));
		config.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.POST.name()));
		config.setAllowedHeaders(List.of(HttpHeaders.CONTENT_TYPE));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
		    .cors().and()
		    .csrf().disable()
		    .authorizeRequests(customizer -> {
		        customizer
		          .antMatchers("/authenticate", "/signin", "/verify-totp",
		              "/verify-totp-additional-security", "/signup",
		              "/signup-confirm-secret").permitAll()
				      .anyRequest().authenticated();
		        })
				.logout(customizer ->
				        customizer.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));
	}

}
