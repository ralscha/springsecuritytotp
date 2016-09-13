package ch.rasc.sec.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.rasc.sec.entity.User;
import ch.rasc.sec.repository.UserRepository;

@Controller
public class LoginController {
	private final UserRepository userRepository;

	LoginController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@RequestMapping("/login")
	public String home(Map<String, Object> model) {

		addModelData(this.userRepository.findByUserName("admin"), model);
		addModelData(this.userRepository.findByUserName("user"), model);
		addModelData(this.userRepository.findByUserName("lazy"), model);

		return "login";
	}

	private static void addModelData(User user, Map<String, Object> model) {
		String key = user.getUserName();
		model.put(key + "FailedLogins", user.getFailedLogins());
		model.put(key + "LockedOut", user.getLockedOut());
		model.put(key + "ExpirationDate", user.getExpirationDate());
	}
}
