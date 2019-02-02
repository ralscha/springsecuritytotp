package ch.rasc.twofactor.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private TotpWebAuthenticationDetailsSource totpWebAuthenticationDetailsSource;

	@Override
	public void configure(WebSecurity builder) throws Exception {
		builder.ignoring().antMatchers("/**/*.png");
	}

    @Override
    protected void configure(HttpSecurity http) throws Exception {
		//@formatter:off
		http
		  .authorizeRequests()
		  .anyRequest()
		  .authenticated()
		.and()
		  .formLogin()
		  .authenticationDetailsSource(this.totpWebAuthenticationDetailsSource)
		  .loginPage("/login")
		  .failureUrl("/login?error")
		  .permitAll()
		.and()
		  .logout()
		    .logoutSuccessUrl("/");
		//@formatter:on

    }

	@Configuration
	@Order(1)
	public static class H2ConsoleSecurityConfigurationAdapter
			extends WebSecurityConfigurerAdapter {
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			//@formatter:off
			http
			  .antMatcher("/h2-console/**")
			    .authorizeRequests()
			      .anyRequest().fullyAuthenticated()
			  .and()
			    .csrf().disable()
			    .headers().frameOptions().sameOrigin();
			//@formatter:on
		}
	}

}