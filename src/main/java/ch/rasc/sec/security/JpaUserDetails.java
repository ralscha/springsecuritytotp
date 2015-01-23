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

		if (user.getLockedOut() != null
				&& user.getLockedOut().isAfter(LocalDateTime.now())) {
			this.locked = true;
		}
		else {
			this.locked = false;
		}

		if (user.getExpirationDate() != null
				&& LocalDateTime.now().isAfter(user.getExpirationDate())) {
			this.expired = true;
		}
		else {
			this.expired = false;
		}

		Set<GrantedAuthority> auths = new HashSet<>();
		for (Role role : user.getRoles()) {
			auths.add(new SimpleGrantedAuthority(role.getName()));
		}

		this.authorities = Collections.unmodifiableCollection(auths);
	}

	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		return this.authorities;
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	@Override
	public String getUsername() {
		return this.username;
	}

	public Long getUserDbId() {
		return this.userDbId;
	}

	@Override
	public boolean isAccountNonExpired() {
		return !this.expired;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !this.locked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	public String getSecret() {
		return this.secret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (this.authorities == null ? 0 : this.authorities.hashCode());
		result = prime * result + (this.enabled ? 1231 : 1237);
		result = prime * result + (this.expired ? 1231 : 1237);
		result = prime * result + (this.locked ? 1231 : 1237);
		result = prime * result + (this.password == null ? 0 : this.password.hashCode());
		result = prime * result + (this.secret == null ? 0 : this.secret.hashCode());
		result = prime * result + (this.userDbId == null ? 0 : this.userDbId.hashCode());
		result = prime * result + (this.username == null ? 0 : this.username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JpaUserDetails other = (JpaUserDetails) obj;
		if (this.authorities == null) {
			if (other.authorities != null) {
				return false;
			}
		}
		else if (!this.authorities.equals(other.authorities)) {
			return false;
		}
		if (this.enabled != other.enabled) {
			return false;
		}
		if (this.expired != other.expired) {
			return false;
		}
		if (this.locked != other.locked) {
			return false;
		}
		if (this.password == null) {
			if (other.password != null) {
				return false;
			}
		}
		else if (!this.password.equals(other.password)) {
			return false;
		}
		if (this.secret == null) {
			if (other.secret != null) {
				return false;
			}
		}
		else if (!this.secret.equals(other.secret)) {
			return false;
		}
		if (this.userDbId == null) {
			if (other.userDbId != null) {
				return false;
			}
		}
		else if (!this.userDbId.equals(other.userDbId)) {
			return false;
		}
		if (this.username == null) {
			if (other.username != null) {
				return false;
			}
		}
		else if (!this.username.equals(other.username)) {
			return false;
		}
		return true;
	}

}
