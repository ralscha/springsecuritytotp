package ch.rasc.sec;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Main extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Main.class);
	}

	public static void main(String[] args) throws Exception {
		// String secret = "K76OJXIWCCCCLEAA";
		// System.out.println(TotpAuthenticatorUtil.verifyCode(secret,749377,2));

		SpringApplication.run(Main.class, args);
	}

	@Bean
	public DataSource dataSource() {
		return DataSourceBuilder.create().driverClassName("org.h2.Driver")
				.url("jdbc:h2:./test").username("sa").password("").build();
	}

}
