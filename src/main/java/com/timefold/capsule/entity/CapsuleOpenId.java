package com.timefold.capsule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CapsuleOpenId implements Serializable {

	private static final long serialVersionUID = 3964646949632921830L;

	@Column(name = "capsule_id")
	private Long capsuleId;

	@Column(name = "user_id")
	private Long userId;

	public CapsuleOpenId() {
	}

	public CapsuleOpenId(Long capsuleId, Long userId) {
		this.capsuleId = capsuleId;
		this.userId = userId;
	}

	public Long getCapsuleId() {
		return capsuleId;
	}

	public void setCapsuleId(Long capsuleId) {
		this.capsuleId = capsuleId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CapsuleOpenId that)) {
			return false;
		}
		return Objects.equals(capsuleId, that.capsuleId) && Objects.equals(userId, that.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(capsuleId, userId);
	}

}