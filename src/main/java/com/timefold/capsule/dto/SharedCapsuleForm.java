package com.timefold.capsule.dto;

import com.timefold.common.enums.VisibilityPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SharedCapsuleForm {

	@NotBlank(message = "Please give your shared capsule a title.")
	@Size(min = 2, max = 200, message = "Title must be between 2 and 200 characters.")
	private String title;

	@Size(max = 1000, message = "Description cannot exceed 1000 characters.")
	private String description;

	@Size(max = 64)
	private String theme = "CELEBRATION";

	@NotNull(message = "Select a visibility policy for contributions.")
	private VisibilityPolicy visibilityPolicy = VisibilityPolicy.ALL_MEMBERS;

	@NotBlank(message = "Select an unlock date and time.")
	private String unlockLocalDateTime;

	@NotBlank(message = "Select a timezone.")
	@Size(max = 64)
	private String timezone = "UTC";

	@Size(max = 50000, message = "Message cannot exceed 50,000 characters.")
	private String initialMessage;

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

	public VisibilityPolicy getVisibilityPolicy() {
		return visibilityPolicy;
	}

	public void setVisibilityPolicy(VisibilityPolicy visibilityPolicy) {
		this.visibilityPolicy = visibilityPolicy;
	}

	public String getUnlockLocalDateTime() {
		return unlockLocalDateTime;
	}

	public void setUnlockLocalDateTime(String unlockLocalDateTime) {
		this.unlockLocalDateTime = unlockLocalDateTime;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public String getInitialMessage() {
		return initialMessage;
	}

	public void setInitialMessage(String initialMessage) {
		this.initialMessage = initialMessage;
	}

}