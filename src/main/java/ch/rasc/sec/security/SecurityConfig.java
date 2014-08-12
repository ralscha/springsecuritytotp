package ch.rasc.sec.security;

import org.h2.server.web.WebServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import scala.annotation.meta.setter;

@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {

	@Bean
	protected WebMvcConfigurerAdapter viewControllers() {
		return new WebMvcConfigurerAdapter() {
			@Override
			public void addViewControllers(ViewControllerRegistry registry) {
				registry.addViewController("/login").setViewName("login");
			}
		};
	}

	@Configuration
	public static class DefaultWebSecurityConfigurerAdapter extends
			WebSecurityConfigurerAdapter {

		@Bean
		public PasswordEncoder passwordEncoder() {
			return new BCryptPasswordEncoder();
		}

		@Autowired
		public void configureGlobal(UserDetailsService userDetailsService,
				AuthenticationManagerBuilder auth) throws Exception {

			TotpAuthenticationConfigurer configurer = new TotpAuthenticationConfigurer(
					userDetailsService).passwordEncoder(passwordEncoder());
			auth.apply(configurer);
		}

		@Override
		public void configure(WebSecurity builder) throws Exception {
			builder.ignoring().antMatchers("/robots.txt", "/**/*.png");
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
			  .authenticationDetailsSource(new TotpWebAuthenticationDetailsSource())
			  .loginPage("/login").failureUrl("/login?error").permitAll()
			.and()
			  .logout()
			  .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"));
			//@formatter:on
		}
	}

	@Configuration
	@Order(1)
	public static class H2ConsoleSecurityConfigurationAdapter extends
			WebSecurityConfigurerAdapter {
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			//@formatter:off
			http
			  .antMatcher("/console/**")
			    .authorizeRequests()
			    .anyRequest()
				.fullyAuthenticated()
			  .and()
			    .csrf().disable()
			    .headers().disable();
			//@formatter:on
		}
	}

	@Bean
	public ServletRegistrationBean h2servletRegistration() {
		WebServlet h2Servlet = new WebServlet();
		h2Servlet.
		ServletRegistrationBean registration = new ServletRegistrationBean(
				h2Servlet);
		registration.addUrlMappings("/console/*");
		return registration;
	}
}