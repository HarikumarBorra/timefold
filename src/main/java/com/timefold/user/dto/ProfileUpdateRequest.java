package com.timefold.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {

	@NotBlank(message = "Full name is required.")
	@Size(min = 2, max = 150)
	private String fullName;

	@Size(max = 100)
	private String displayName;

	@Pattern(regexp = "^$|^\\+[1-9]\\d{6,14}$", message = "Phone number must be in E.164 format (e.g., +919876543210).")
	private String phoneE164;

	@NotBlank(message = "Default timezone is required.")
	@Size(max = 64)
	private String defaultTimezone = "UTC";

	@Size(max = 500)
	private String bio;

	private boolean emailEnabled = true;
	private boolean upcomingUnlockReminders = true;
	private boolean occasionReminders = true;

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
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

	public String getDefaultTimezone() {
		return defaultTimezone;
	}

	public void setDefaultTimezone(String defaultTimezone) {
		this.defaultTimezone = defaultTimezone;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public boolean isEmailEnabled() {
		return emailEnabled;
	}

	public void setEmailEnabled(boolean emailEnabled) {
		this.emailEnabled = emailEnabled;
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

}