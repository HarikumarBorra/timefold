package com.timefold.user.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.timefold.common.enums.AccountStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "public_id", nullable = false, unique = true, length = 36, columnDefinition = "CHAR(36)")
	private String publicId;

	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;

	@Column(name = "email", nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_status", nullable = false, columnDefinition = "ENUM('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'DELETED')")
	private AccountStatus accountStatus = AccountStatus.PENDING_VERIFICATION;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified = false;

	@Column(name = "email_verified_at_utc")
	private Instant emailVerifiedAtUtc;

	@Column(name = "last_login_at_utc")
	private Instant lastLoginAtUtc;

	@Column(name = "deleted_at_utc")
	private Instant deletedAtUtc;

	@Version
	@Column(name = "version", nullable = false)
	private Long version = 0L;

	@Column(name = "created_at_utc", nullable = false, updatable = false)
	private Instant createdAtUtc;

	@Column(name = "updated_at_utc", nullable = false)
	private Instant updatedAtUtc;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	@PrePersist
	protected void onCreate() {
		Instant now = Instant.now();
		if (this.publicId == null) {
			this.publicId = UUID.randomUUID().toString();
		}
		if (this.createdAtUtc == null) {
			this.createdAtUtc = now;
		}
		this.updatedAtUtc = now;
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAtUtc = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getPublicId() {
		return publicId;
	}

	public void setPublicId(String publicId) {
		this.publicId = publicId;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
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

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public Instant getEmailVerifiedAtUtc() {
		return emailVerifiedAtUtc;
	}

	public void setEmailVerifiedAtUtc(Instant emailVerifiedAtUtc) {
		this.emailVerifiedAtUtc = emailVerifiedAtUtc;
	}

	public Instant getLastLoginAtUtc() {
		return lastLoginAtUtc;
	}

	public void setLastLoginAtUtc(Instant lastLoginAtUtc) {
		this.lastLoginAtUtc = lastLoginAtUtc;
	}

	public Instant getDeletedAtUtc() {
		return deletedAtUtc;
	}

	public void setDeletedAtUtc(Instant deletedAtUtc) {
		this.deletedAtUtc = deletedAtUtc;
	}

	public Long getVersion() {
		return version;
	}

	public Instant getCreatedAtUtc() {
		return createdAtUtc;
	}

	public Instant getUpdatedAtUtc() {
		return updatedAtUtc;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void addRole(Role role) {
		this.roles.add(role);
	}

}