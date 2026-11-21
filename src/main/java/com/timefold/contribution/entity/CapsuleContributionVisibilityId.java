package com.timefold.contribution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CapsuleContributionVisibilityId implements Serializable {

	private static final long serialVersionUID = -1717331627448077264L;

	@Column(name = "contribution_id")
	private Long contributionId;

	@Column(name = "allowed_user_id")
	private Long allowedUserId;

	public CapsuleContributionVisibilityId() {
	}

	public CapsuleContributionVisibilityId(Long contributionId, Long allowedUserId) {
		this.contributionId = contributionId;
		this.allowedUserId = allowedUserId;
	}

	public Long getContributionId() {
		return contributionId;
	}

	public void setContributionId(Long contributionId) {
		this.contributionId = contributionId;
	}

	public Long getAllowedUserId() {
		return allowedUserId;
	}

	public void setAllowedUserId(Long allowedUserId) {
		this.allowedUserId = allowedUserId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CapsuleContributionVisibilityId that)) {
			return false;
		}
		return Objects.equals(contributionId, that.contributionId) && Objects.equals(allowedUserId, that.allowedUserId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(contributionId, allowedUserId);
	}

}