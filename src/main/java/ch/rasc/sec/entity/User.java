package ch.rasc.sec.entity;

import java.time.LocalDateTime;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;
import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Table(name = "AppUser")
public class User extends AbstractPersistable<Long> {

	private static final long serialVersionUID = 1L;

	@NotNull
	@Size(max = 100)
	@Column(unique = true)
	private String userName;

	@Size(max = 255)
	private String name;

	@Size(max = 255)
	private String firstName;

	@Email
	@Size(max = 255)
	@NotNull
	@Column(unique = true)
	private String email;

	@Size(max = 80)
	private String passwordHash;

	@Size(max = 8)
	private String locale;

	private boolean enabled;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "AppUserRoles", joinColumns = @JoinColumn(name = "userId"), inverseJoinColumns = @JoinColumn(name = "roleId"))
	private Set<Role> roles;

	private Integer failedLogins;

	private LocalDateTime lockedOut;

	private LocalDateTime expirationDate;

	@Size(max = 16)
	private String secret;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public Integer getFailedLogins() {
		return failedLogins;
	}

	public void setFailedLogins(Integer failedLogins) {
		this.failedLogins = failedLogins;
	}

	public LocalDateTime getLockedOut() {
		return lockedOut;
	}

	public void setLockedOut(LocalDateTime lockedOut) {
		this.lockedOut = lockedOut;
	}

	public LocalDateTime getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(LocalDateTime expirationDate) {
		this.expirationDate = expirationDate;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

}
