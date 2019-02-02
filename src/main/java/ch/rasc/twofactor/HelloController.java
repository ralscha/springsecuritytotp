package ch.rasc.twofactor;

import java.util.Date;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.rasc.twofactor.security.JooqUserDetails;

@Controller
public class HelloController {

	@GetMapping("/sayHello")
	@ResponseBody
	public String sayHello(@AuthenticationPrincipal JooqUserDetails user) {
		return "Hello " + user.getUsername();
	}

	@GetMapping("/")
	public String home(Map<String, Object> model,
			@AuthenticationPrincipal JooqUserDetails user) {
		model.put("message", "Hello World");
		model.put("title", "Hello Home");
		model.put("date", new Date());
		model.put("user", user.getUsername());
		return "home";
	}
}
