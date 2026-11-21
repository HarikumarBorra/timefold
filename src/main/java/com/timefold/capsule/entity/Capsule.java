package com.timefold.capsule.entity;

import com.timefold.common.enums.CapsuleStatus;
import com.timefold.common.enums.CapsuleType;
import com.timefold.common.enums.VisibilityPolicy;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "capsules")
public class Capsule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "public_id", nullable = false, unique = true, length = 36, columnDefinition = "CHAR(36)")
	private String publicId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_user_id", nullable = false)
	private User owner;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "description", length = 1000)
	private String description;

	@Column(name = "theme", length = 64)
	private String theme;

	@Enumerated(EnumType.STRING)
	@Column(name = "capsule_type", nullable = false, columnDefinition = "ENUM('PERSONAL', 'SHARED', 'RECIPIENT', 'OCCASION')")
	private CapsuleType capsuleType;

	@Enumerated(EnumType.STRING)
	@Column(name = "visibility_policy", nullable = false, columnDefinition = "ENUM('ALL_MEMBERS', 'SELF_ONLY', 'OWNER_DEFINED')")
	private VisibilityPolicy visibilityPolicy = VisibilityPolicy.ALL_MEMBERS;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, columnDefinition = "ENUM('DRAFT', 'SEALED', 'READY', 'OPENED', 'CANCELLED')")
	private CapsuleStatus status = CapsuleStatus.DRAFT;

	@Column(name = "unlock_at_utc", nullable = false)
	private Instant unlockAtUtc;

	@Column(name = "timezone", nullable = false, length = 64)
	private String timezone = "UTC";

	@Column(name = "sealed_at_utc")
	private Instant sealedAtUtc;

	@Column(name = "unlocked_at_utc")
	private Instant unlockedAtUtc;

	@Column(name = "opened_at_utc")
	private Instant openedAtUtc;

	@Column(name = "cancelled_at_utc")
	private Instant cancelledAtUtc;

	@Version
	@Column(name = "version", nullable = false)
	private Long version = 0L;

	@Column(name = "created_at_utc", nullable = false, updatable = false)
	private Instant createdAtUtc;

	@Column(name = "updated_at_utc", nullable = false)
	private Instant updatedAtUtc;

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

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTheme() {
		return theme;
	}

	public void setTheme(String theme) {
		this.theme = theme;
	}

	public CapsuleType getCapsuleType() {
		return capsuleType;
	}

	public void setCapsuleType(CapsuleType capsuleType) {
		this.capsuleType = capsuleType;
	}

	public VisibilityPolicy getVisibilityPolicy() {
		return visibilityPolicy;
	}

	public void setVisibilityPolicy(VisibilityPolicy visibilityPolicy) {
		this.visibilityPolicy = visibilityPolicy;
	}

	public CapsuleStatus getStatus() {
		return status;
	}

	public void setStatus(CapsuleStatus status) {
		this.status = status;
	}

	public Instant getUnlockAtUtc() {
		return unlockAtUtc;
	}

	public void setUnlockAtUtc(Instant unlockAtUtc) {
		this.unlockAtUtc = unlockAtUtc;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public Instant getSealedAtUtc() {
		return sealedAtUtc;
	}

	public void setSealedAtUtc(Instant sealedAtUtc) {
		this.sealedAtUtc = sealedAtUtc;
	}

	public Instant getUnlockedAtUtc() {
		return unlockedAtUtc;
	}

	public void setUnlockedAtUtc(Instant unlockedAtUtc) {
		this.unlockedAtUtc = unlockedAtUtc;
	}

	public Instant getOpenedAtUtc() {
		return openedAtUtc;
	}

	public void setOpenedAtUtc(Instant openedAtUtc) {
		this.openedAtUtc = openedAtUtc;
	}

	public Instant getCancelledAtUtc() {
		return cancelledAtUtc;
	}

	public void setCancelledAtUtc(Instant cancelledAtUtc) {
		this.cancelledAtUtc = cancelledAtUtc;
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

}