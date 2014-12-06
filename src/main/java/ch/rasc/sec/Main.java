package ch.rasc.sec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;

@SpringBootApplication
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

}
