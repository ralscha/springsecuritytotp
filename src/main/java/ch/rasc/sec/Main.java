package ch.rasc.sec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

	public static void main(String[] args) throws Exception {
		// String secret = "K76OJXIWCCCCLEAA";
		// System.out.println(TotpAuthenticatorUtil.verifyCode(secret,749377,2));

		SpringApplication.run(Main.class, args);
	}

}
