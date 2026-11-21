package com.timefold.invitation.entity;

import com.timefold.capsule.entity.Capsule;
import com.timefold.common.enums.CapsuleInvitationStatus;
import com.timefold.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "capsule_invitations")
public class CapsuleInvitation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "capsule_id", nullable = false)
	private Capsule capsule;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "invited_by_user_id", nullable = false)
	private User invitedBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invited_user_id")
	private User invitedUser;

	@Column(name = "invitee_email", nullable = false, length = 255)
	private String inviteeEmail;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "token_hash", nullable = false, unique = true, length = 64, columnDefinition = "CHAR(64)")
	private String tokenHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, columnDefinition = "ENUM('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'REVOKED')")
	private CapsuleInvitationStatus status = CapsuleInvitationStatus.PENDING;

	@Column(name = "expires_at_utc", nullable = false)
	private Instant expiresAtUtc;

	@Column(name = "responded_at_utc")
	private Instant respondedAtUtc;

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

	public Capsule getCapsule() {
		return capsule;
	}

	public void setCapsule(Capsule capsule) {
		this.capsule = capsule;
	}

	public User getInvitedBy() {
		return invitedBy;
	}

	public void setInvitedBy(User invitedBy) {
		this.invitedBy = invitedBy;
	}

	public User getInvitedUser() {
		return invitedUser;
	}

	public void setInvitedUser(User invitedUser) {
		this.invitedUser = invitedUser;
	}

	public String getInviteeEmail() {
		return inviteeEmail;
	}

	public void setInviteeEmail(String inviteeEmail) {
		this.inviteeEmail = inviteeEmail;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public CapsuleInvitationStatus getStatus() {
		return status;
	}

	public void setStatus(CapsuleInvitationStatus status) {
		this.status = status;
	}

	public Instant getExpiresAtUtc() {
		return expiresAtUtc;
	}

	public void setExpiresAtUtc(Instant expiresAtUtc) {
		this.expiresAtUtc = expiresAtUtc;
	}

	public Instant getRespondedAtUtc() {
		return respondedAtUtc;
	}

	public void setRespondedAtUtc(Instant respondedAtUtc) {
		this.respondedAtUtc = respondedAtUtc;
	}

	public Instant getCreatedAtUtc() {
		return createdAtUtc;
	}

}