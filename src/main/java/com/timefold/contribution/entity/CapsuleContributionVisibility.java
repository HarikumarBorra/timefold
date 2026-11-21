package com.timefold.contribution.entity;

import com.timefold.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "capsule_contribution_visibility")
public class CapsuleContributionVisibility {

	@EmbeddedId
	private CapsuleContributionVisibilityId id = new CapsuleContributionVisibilityId();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("contributionId")
	@JoinColumn(name = "contribution_id", nullable = false)
	private CapsuleContribution contribution;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("allowedUserId")
	@JoinColumn(name = "allowed_user_id", nullable = false)
	private User allowedUser;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "granted_by_user_id", nullable = false)
	private User grantedBy;

	@Column(name = "created_at_utc", nullable = false, updatable = false)
	private Instant createdAtUtc;

	@PrePersist
	protected void onCreate() {
		if (this.createdAtUtc == null) {
			this.createdAtUtc = Instant.now();
		}
	}

	public CapsuleContributionVisibilityId getId() {
		return id;
	}

	public void setId(CapsuleContributionVisibilityId id) {
		this.id = id;
	}

	public CapsuleContribution getContribution() {
		return contribution;
	}

	public void setContribution(CapsuleContribution contribution) {
		this.contribution = contribution;
	}

	public User getAllowedUser() {
		return allowedUser;
	}

	public void setAllowedUser(User allowedUser) {
		this.allowedUser = allowedUser;
	}

	public User getGrantedBy() {
		return grantedBy;
	}

	public void setGrantedBy(User grantedBy) {
		this.grantedBy = grantedBy;
	}

	public Instant getCreatedAtUtc() {
		return createdAtUtc;
	}

}