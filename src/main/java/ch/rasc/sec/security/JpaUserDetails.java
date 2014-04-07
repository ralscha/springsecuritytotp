package ch.rasc.sec.security;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ch.rasc.sec.entity.Role;
import ch.rasc.sec.entity.User;

public class JpaUserDetails implements UserDetails {

	private static final long serialVersionUID = 1L;

	private final Collection<GrantedAuthority> authorities;

	private final String password;

	private final String username;

	private final String secret;

	private final boolean enabled;

	private final Long userDbId;

	private final boolean locked;

	private final boolean expired;

	public JpaUserDetails(User user) {
		this.userDbId = user.getId();

		this.password = user.getPasswordHash();
		this.username = user.getUserName();
		this.secret = user.getSecret();
		this.enabled = user.isEnabled();

		if (user.getLockedOut() != null && user.getLockedOut().isAfter(LocalDateTime.now())) {
			locked = true;
		} else {
			locked = false;
		}

		if (user.getExpirationDate() != null && LocalDateTime.now().isAfter(user.getExpirationDate())) {
			expired = true;
		} else {
			expired = false;
		}

		Set<GrantedAuthority> auths = new HashSet<>();
		for (Role role : user.getRoles()) {
			auths.add(new SimpleGrantedAuthority(role.getName()));
		}

		this.authorities = Collections.unmodifiableCollection(auths);
	}

	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	public Long getUserDbId() {
		return userDbId;
	}

	@Override
	public boolean isAccountNonExpired() {
		return !expired;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !locked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public String getSecret() {
		return secret;
	}

}
