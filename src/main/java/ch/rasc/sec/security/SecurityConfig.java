package ch.rasc.sec.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Autowired
	public void configureGlobal(UserDetailsService userDetailsService, AuthenticationManagerBuilder auth)
			throws Exception {

		TotpAuthenticationConfigurer configurer = new TotpAuthenticationConfigurer(userDetailsService)
				.passwordEncoder(passwordEncoder());
		auth.apply(configurer);
	}

	@Override
	public void configure(WebSecurity builder) throws Exception {
		builder.ignoring().antMatchers("/robots.txt");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		//@formatter:off
		http
		  .authorizeRequests()
		    .anyRequest().authenticated()
		  .and()
		  .formLogin()
		    .authenticationDetailsSource(new TotpWebAuthenticationDetailsSource())
		    .loginPage("/login.jsp")
			.failureUrl("/login.jsp?error")
			.permitAll();
		//@formatter:on
	}

}