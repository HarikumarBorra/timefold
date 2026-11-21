package com.timefold.contribution.entity;

import com.timefold.capsule.entity.Capsule;
import com.timefold.common.enums.ContributionStatus;
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
@Table(name = "capsule_contributions")
public class CapsuleContribution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "public_id", nullable = false, unique = true, length = 36, columnDefinition = "CHAR(36)")
	private String publicId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "capsule_id", nullable = false)
	private Capsule capsule;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "contributor_user_id", nullable = false)
	private User contributor;

	@Column(name = "title", length = 200)
	private String title;

	@Column(name = "message_content", nullable = false, columnDefinition = "MEDIUMTEXT")
	private String messageContent;

	@Column(name = "encryption_key_version")
	private Short encryptionKeyVersion;

	@Column(name = "encryption_iv", columnDefinition = "VARBINARY(16)")
	private byte[] encryptionIv;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, columnDefinition = "ENUM('DRAFT', 'SUBMITTED', 'WITHDRAWN')")
	private ContributionStatus status = ContributionStatus.SUBMITTED;

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

	public Capsule getCapsule() {
		return capsule;
	}

	public void setCapsule(Capsule capsule) {
		this.capsule = capsule;
	}

	public User getContributor() {
		return contributor;
	}

	public void setContributor(User contributor) {
		this.contributor = contributor;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessageContent() {
		return messageContent;
	}

	public void setMessageContent(String messageContent) {
		this.messageContent = messageContent;
	}

	public Short getEncryptionKeyVersion() {
		return encryptionKeyVersion;
	}

	public void setEncryptionKeyVersion(Short encryptionKeyVersion) {
		this.encryptionKeyVersion = encryptionKeyVersion;
	}

	public byte[] getEncryptionIv() {
		return encryptionIv;
	}

	public void setEncryptionIv(byte[] encryptionIv) {
		this.encryptionIv = encryptionIv;
	}

	public ContributionStatus getStatus() {
		return status;
	}

	public void setStatus(ContributionStatus status) {
		this.status = status;
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