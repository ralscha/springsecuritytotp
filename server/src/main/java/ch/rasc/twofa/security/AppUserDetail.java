package ch.rasc.twofa.security;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

import ch.rasc.twofa.db.tables.records.AppUserRecord;

public class AppUserDetail implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Long appUserId;

	private final String username;

	private final boolean enabled;

	private final Set<GrantedAuthority> authorities;

	public AppUserDetail(AppUserRecord user) {
		this.appUserId = user.getId();
		this.username = user.getUsername();
		this.authorities = Set.of();
		this.enabled = Objects.requireNonNullElse(user.getEnabled(), false);
	}

	public Long getAppUserId() {
		return this.appUserId;
	}

	public String getUsername() {
		return this.username;
	}

	public Set<GrantedAuthority> getAuthorities() {
		return this.authorities;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

}
