package com.timefold.notification.entity;

import com.timefold.user.entity.User;
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
@Table(name = "notification_preferences")
public class NotificationPreference {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "email_enabled", nullable = false)
	private boolean emailEnabled = true;

	@Column(name = "whatsapp_enabled", nullable = false)
	private boolean whatsappEnabled = false;

	@Column(name = "whatsapp_opt_in_at_utc")
	private Instant whatsappOptInAtUtc;

	@Column(name = "whatsapp_revoked_at_utc")
	private Instant whatsappRevokedAtUtc;

	@Column(name = "upcoming_unlock_reminders", nullable = false)
	private boolean upcomingUnlockReminders = true;

	@Column(name = "occasion_reminders", nullable = false)
	private boolean occasionReminders = true;

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

	public boolean isEmailEnabled() {
		return emailEnabled;
	}

	public void setEmailEnabled(boolean emailEnabled) {
		this.emailEnabled = emailEnabled;
	}

	public boolean isWhatsappEnabled() {
		return whatsappEnabled;
	}

	public void setWhatsappEnabled(boolean whatsappEnabled) {
		this.whatsappEnabled = whatsappEnabled;
	}

	public Instant getWhatsappOptInAtUtc() {
		return whatsappOptInAtUtc;
	}

	public void setWhatsappOptInAtUtc(Instant whatsappOptInAtUtc) {
		this.whatsappOptInAtUtc = whatsappOptInAtUtc;
	}

	public Instant getWhatsappRevokedAtUtc() {
		return whatsappRevokedAtUtc;
	}

	public void setWhatsappRevokedAtUtc(Instant whatsappRevokedAtUtc) {
		this.whatsappRevokedAtUtc = whatsappRevokedAtUtc;
	}

	public boolean isUpcomingUnlockReminders() {
		return upcomingUnlockReminders;
	}

	public void setUpcomingUnlockReminders(boolean upcomingUnlockReminders) {
		this.upcomingUnlockReminders = upcomingUnlockReminders;
	}

	public boolean isOccasionReminders() {
		return occasionReminders;
	}

	public void setOccasionReminders(boolean occasionReminders) {
		this.occasionReminders = occasionReminders;
	}

	public Instant getUpdatedAtUtc() {
		return updatedAtUtc;
	}

}