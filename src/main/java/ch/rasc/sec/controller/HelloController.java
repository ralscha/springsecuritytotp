package ch.rasc.sec.controller;

import java.util.Date;
import java.util.Map;

import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.rasc.sec.security.JpaUserDetails;

@Controller
public class HelloController {

	@RequestMapping(value = "/sayHello", method = RequestMethod.GET)
	@ResponseBody
	public String sayHello(@AuthenticationPrincipal JpaUserDetails user) {
		return "Hello " + user.getUsername();
	}

	@RequestMapping("/")
	public String home(Map<String, Object> model,
			@AuthenticationPrincipal JpaUserDetails user) {
		model.put("message", "Hello World");
		model.put("title", "Hello Home");
		model.put("date", new Date());
		model.put("user", user.getUsername());
		return "home";
	}
}
