package com.timefold.notification.email;

public interface AuthEmailSender {

	void sendVerificationEmail(String toEmail, String fullName, String verificationLink);

	void sendPasswordResetEmail(String toEmail, String fullName, String resetLink);

}