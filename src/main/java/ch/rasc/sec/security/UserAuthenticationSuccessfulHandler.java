package ch.rasc.sec.security;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.rasc.sec.entity.User;

@Component
public class UserAuthenticationSuccessfulHandler
		implements ApplicationListener<InteractiveAuthenticationSuccessEvent> {

	private final EntityManager entityManager;

	public UserAuthenticationSuccessfulHandler(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	@Transactional
	public void onApplicationEvent(InteractiveAuthenticationSuccessEvent event) {
		Object principal = event.getAuthentication().getPrincipal();
		if (principal instanceof JpaUserDetails) {
			User user = this.entityManager.find(User.class,
					((JpaUserDetails) principal).getUserDbId());
			user.setLockedOut(null);
			user.setFailedLogins(null);
			user.setExpirationDate(LocalDateTime.now().plusYears(1));
		}
	}
}