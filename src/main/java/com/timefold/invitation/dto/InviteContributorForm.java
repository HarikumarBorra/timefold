package com.timefold.invitation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InviteContributorForm {

	@NotBlank(message = "Enter the contributor's email address.")
	@Email(message = "Please enter a valid email address.")
	@Size(max = 255)
	private String inviteeEmail;

	public String getInviteeEmail() {
		return inviteeEmail;
	}

	public void setInviteeEmail(String inviteeEmail) {
		this.inviteeEmail = inviteeEmail;
	}

}