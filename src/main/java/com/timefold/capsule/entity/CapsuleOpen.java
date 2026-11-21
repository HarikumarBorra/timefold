package com.timefold.capsule.entity;

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
@Table(name = "capsule_opens")
public class CapsuleOpen {

	@EmbeddedId
	private CapsuleOpenId id = new CapsuleOpenId();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("capsuleId")
	@JoinColumn(name = "capsule_id", nullable = false)
	private Capsule capsule;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("userId")
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "first_opened_at_utc", nullable = false, updatable = false)
	private Instant firstOpenedAtUtc;

	@Column(name = "last_opened_at_utc", nullable = false)
	private Instant lastOpenedAtUtc;

	@Column(name = "open_count", nullable = false)
	private Integer openCount = 1;

	@PrePersist
	protected void onCreate() {
		Instant now = Instant.now();
		if (this.firstOpenedAtUtc == null) {
			this.firstOpenedAtUtc = now;
		}
		if (this.lastOpenedAtUtc == null) {
			this.lastOpenedAtUtc = now;
		}
		if (this.openCount == null || this.openCount < 1) {
			this.openCount = 1;
		}
	}

	public CapsuleOpenId getId() {
		return id;
	}

	public void setId(CapsuleOpenId id) {
		this.id = id;
	}

	public Capsule getCapsule() {
		return capsule;
	}

	public void setCapsule(Capsule capsule) {
		this.capsule = capsule;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Instant getFirstOpenedAtUtc() {
		return firstOpenedAtUtc;
	}

	public void setFirstOpenedAtUtc(Instant firstOpenedAtUtc) {
		this.firstOpenedAtUtc = firstOpenedAtUtc;
	}

	public Instant getLastOpenedAtUtc() {
		return lastOpenedAtUtc;
	}

	public void setLastOpenedAtUtc(Instant lastOpenedAtUtc) {
		this.lastOpenedAtUtc = lastOpenedAtUtc;
	}

	public Integer getOpenCount() {
		return openCount;
	}

	public void setOpenCount(Integer openCount) {
		this.openCount = openCount;
	}

}