package ch.rasc.sec.controller;

import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.rasc.sec.security.JpaUserDetails;

@Controller
public class HelloController {

	@RequestMapping(value = "/", method = RequestMethod.GET)
	@ResponseBody
	public String sayHello(@AuthenticationPrincipal JpaUserDetails user) {
		return "Hello " + user.getUsername();
	}

}
