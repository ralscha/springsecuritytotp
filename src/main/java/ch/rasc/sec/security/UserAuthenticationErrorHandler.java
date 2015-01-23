package ch.rasc.sec.security;

import java.time.LocalDateTime;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.rasc.sec.entity.User;
import ch.rasc.sec.repository.UserRepository;

@Component
public class UserAuthenticationErrorHandler implements
		ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

	private final UserRepository userRepository;

	@Autowired
	public UserAuthenticationErrorHandler(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional
	public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
		Object principal = event.getAuthentication().getPrincipal();
		if (principal instanceof String) {
			User user = this.userRepository.findByUserName((String) principal);
			if (user != null) {
				if (user.getFailedLogins() == null) {
					user.setFailedLogins(1);
				}
				else {
					user.setFailedLogins(user.getFailedLogins() + 1);
				}

				if (user.getFailedLogins() > 10) {
					user.setLockedOut(LocalDateTime.now().plusMinutes(10));
				}

			}
			else {
				LoggerFactory.getLogger(UserAuthenticationErrorHandler.class).error(
						"Unknown user login attempt: {}", principal);
			}
		}
	}
}