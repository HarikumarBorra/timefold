package com.timefold.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "display_name", length = 100)
	private String displayName;

	@Column(name = "phone_e164", unique = true, length = 20)
	private String phoneE164;

	@Column(name = "phone_verified", nullable = false)
	private boolean phoneVerified = false;

	@Column(name = "phone_verified_at_utc")
	private Instant phoneVerifiedAtUtc;

	@Column(name = "default_timezone", nullable = false, length = 64)
	private String defaultTimezone = "UTC";

	@Column(name = "locale", nullable = false, length = 16)
	private String locale = "en";

	@Column(name = "avatar_url", length = 512)
	private String avatarUrl;

	@Column(name = "bio", length = 500)
	private String bio;

	@Column(name = "updated_at_utc", nullable = false)
	private Instant updatedAtUtc;

	@PrePersist
	@PreUpdate
	protected void onTouch() {
		this.updatedAtUtc = Instant.now();
	}

	public Long getUserId() {
		return userId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getPhoneE164() {
		return phoneE164;
	}

	public void setPhoneE164(String phoneE164) {
		this.phoneE164 = phoneE164;
	}

	public boolean isPhoneVerified() {
		return phoneVerified;
	}

	public void setPhoneVerified(boolean phoneVerified) {
		this.phoneVerified = phoneVerified;
	}

	public Instant getPhoneVerifiedAtUtc() {
		return phoneVerifiedAtUtc;
	}

	public void setPhoneVerifiedAtUtc(Instant phoneVerifiedAtUtc) {
		this.phoneVerifiedAtUtc = phoneVerifiedAtUtc;
	}

	public String getDefaultTimezone() {
		return defaultTimezone;
	}

	public void setDefaultTimezone(String defaultTimezone) {
		this.defaultTimezone = defaultTimezone;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public Instant getUpdatedAtUtc() {
		return updatedAtUtc;
	}

}