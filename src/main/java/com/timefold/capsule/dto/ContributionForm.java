package com.timefold.capsule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContributionForm {

	@Size(max = 200, message = "Optional note title cannot exceed 200 characters.")
	private String title;

	@NotBlank(message = "Please write your contribution message.")
	@Size(min = 1, max = 50000, message = "Message cannot exceed 50,000 characters.")
	private String messageContent;

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

}