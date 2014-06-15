package ch.rasc.sec;

import java.time.LocalDateTime;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
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

		if (roleRepository.count() == 0) {
			adminRole = new Role();
			adminRole.setName("ADMIN");
			roleRepository.save(adminRole);

			userRole = new Role();
			userRole.setName("USER");
			roleRepository.save(userRole);
		}
		else {
			adminRole = roleRepository.findByName("ADMIN");
			userRole = roleRepository.findByName("USER");
		}

		if (userRepository.count() == 0) {
			// admin user
			User adminUser = new User();
			adminUser.setUserName("admin");
			adminUser.setEmail("test@test.ch");
			adminUser.setFirstName("admin");
			adminUser.setName("admin");
			adminUser.setLocale("en");
			adminUser.setPasswordHash(passwordEncoder.encode("admin"));
			adminUser.setEnabled(true);
			adminUser.setExpirationDate(LocalDateTime.now().plusYears(1));
			adminUser.setSecret("IB6EFEQKE7U2TQIB");

			adminUser.setRoles(Collections.singleton(adminRole));

			userRepository.save(adminUser);

			// normal user
			User normalUser = new User();
			normalUser.setUserName("user");
			normalUser.setEmail("user@test.ch");
			normalUser.setFirstName("user");
			normalUser.setName("user");
			normalUser.setLocale("de");
			normalUser.setExpirationDate(LocalDateTime.now().plusYears(1));
			normalUser.setSecret("BPPGGZTFHRWDUA67");

			normalUser.setPasswordHash(passwordEncoder.encode("user"));
			normalUser.setEnabled(true);

			normalUser.setRoles(Collections.singleton(userRole));

			userRepository.save(normalUser);
		}

	}

}
