package com.timefold.auth.entity;

import com.timefold.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "token_hash", nullable = false, unique = true, length = 64, columnDefinition = "CHAR(64)")
	private String tokenHash;

	@Column(name = "expires_at_utc", nullable = false)
	private Instant expiresAtUtc;

	@Column(name = "used_at_utc")
	private Instant usedAtUtc;

	@Column(name = "request_ip", length = 45)
	private String requestIp;

	@Column(name = "created_at_utc", nullable = false, updatable = false)
	private Instant createdAtUtc;

	@PrePersist
	protected void onCreate() {
		if (this.createdAtUtc == null) {
			this.createdAtUtc = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public Instant getExpiresAtUtc() {
		return expiresAtUtc;
	}

	public void setExpiresAtUtc(Instant expiresAtUtc) {
		this.expiresAtUtc = expiresAtUtc;
	}

	public Instant getUsedAtUtc() {
		return usedAtUtc;
	}

	public void setUsedAtUtc(Instant usedAtUtc) {
		this.usedAtUtc = usedAtUtc;
	}

	public String getRequestIp() {
		return requestIp;
	}

	public void setRequestIp(String requestIp) {
		this.requestIp = requestIp;
	}

	public Instant getCreatedAtUtc() {
		return createdAtUtc;
	}

}