package ch.rasc.sec;

import java.time.LocalDateTime;
import java.util.Collections;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.rasc.sec.entity.Role;
import ch.rasc.sec.entity.User;
import ch.rasc.sec.repository.RoleRepository;
import ch.rasc.sec.repository.UserRepository;

@Component
public class InitDatabase implements ApplicationListener<ContextRefreshedEvent> {

	private final PasswordEncoder passwordEncoder;

	private final UserRepository userRepository;

	private final RoleRepository roleRepository;

	public InitDatabase(PasswordEncoder passwordEncoder, UserRepository userRepository,
			RoleRepository roleRepository) {
		this.passwordEncoder = passwordEncoder;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
	}

	@Override
	@Transactional
	public void onApplicationEvent(ContextRefreshedEvent event) {

		Role adminRole = null;
		Role userRole = null;

		if (this.roleRepository.count() == 0) {
			adminRole = new Role();
			adminRole.setName("ADMIN");
			this.roleRepository.save(adminRole);

			userRole = new Role();
			userRole.setName("USER");
			this.roleRepository.save(userRole);
		}
		else {
			adminRole = this.roleRepository.findByName("ADMIN");
			userRole = this.roleRepository.findByName("USER");
		}

		if (this.userRepository.count() == 0) {
			// admin user
			User adminUser = new User();
			adminUser.setUserName("admin");
			adminUser.setEmail("test@test.ch");
			adminUser.setFirstName("admin");
			adminUser.setName("admin");
			adminUser.setLocale("en");
			adminUser.setPasswordHash(this.passwordEncoder.encode("admin"));
			adminUser.setEnabled(true);
			adminUser.setExpirationDate(LocalDateTime.now().plusYears(1));
			adminUser.setSecret("IB6EFEQKE7U2TQIB");

			adminUser.setRoles(Collections.singleton(adminRole));

			this.userRepository.save(adminUser);

			// normal user
			User normalUser = new User();
			normalUser.setUserName("user");
			normalUser.setEmail("user@test.ch");
			normalUser.setFirstName("user");
			normalUser.setName("user");
			normalUser.setLocale("de");
			normalUser.setExpirationDate(LocalDateTime.now().plusYears(1));
			normalUser.setSecret("BPPGGZTFHRWDUA67");

			normalUser.setPasswordHash(this.passwordEncoder.encode("user"));
			normalUser.setEnabled(true);

			normalUser.setRoles(Collections.singleton(userRole));

			this.userRepository.save(normalUser);

			// lazy user, user without authenticator
			User lazyUser = new User();
			lazyUser.setUserName("lazy");
			lazyUser.setEmail("lazy@test.ch");
			lazyUser.setFirstName("lazy");
			lazyUser.setName("lazy");
			lazyUser.setLocale("en");
			lazyUser.setExpirationDate(LocalDateTime.now().plusYears(1));
			lazyUser.setSecret(null);
			lazyUser.setPasswordHash(this.passwordEncoder.encode("lazy"));
			lazyUser.setEnabled(true);
			lazyUser.setRoles(Collections.singleton(userRole));
			this.userRepository.save(lazyUser);
		}

	}

}
