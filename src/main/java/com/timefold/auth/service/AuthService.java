package com.timefold.auth.service;

import com.timefold.audit.service.AuditService;
import com.timefold.auth.dto.RegisterRequest;
import com.timefold.auth.dto.ResetPasswordRequest;
import com.timefold.auth.entity.EmailVerification;
import com.timefold.auth.entity.PasswordResetToken;
import com.timefold.auth.repository.EmailVerificationRepository;
import com.timefold.auth.repository.PasswordResetTokenRepository;
import com.timefold.common.enums.AccountStatus;
import com.timefold.common.exception.BusinessException;
import com.timefold.common.util.TokenUtil;
import com.timefold.config.AppProperties;
import com.timefold.notification.email.AuthEmailSender;
import com.timefold.notification.entity.NotificationPreference;
import com.timefold.notification.repository.NotificationPreferenceRepository;
import com.timefold.user.entity.Role;
import com.timefold.user.entity.User;
import com.timefold.user.entity.UserProfile;
import com.timefold.user.repository.RoleRepository;
import com.timefold.user.repository.UserProfileRepository;
import com.timefold.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final Duration EMAIL_VERIFICATION_TTL = Duration.ofHours(24);
	private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserProfileRepository userProfileRepository;
	private final NotificationPreferenceRepository notificationPreferenceRepository;
	private final EmailVerificationRepository emailVerificationRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthEmailSender authEmailSender;
	private final AuditService auditService;
	private final AppProperties appProperties;

	public AuthService(UserRepository userRepository, RoleRepository roleRepository,
			UserProfileRepository userProfileRepository,
			NotificationPreferenceRepository notificationPreferenceRepository,
			EmailVerificationRepository emailVerificationRepository,
			PasswordResetTokenRepository passwordResetTokenRepository, PasswordEncoder passwordEncoder,
			AuthEmailSender authEmailSender, AuditService auditService, AppProperties appProperties) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userProfileRepository = userProfileRepository;
		this.notificationPreferenceRepository = notificationPreferenceRepository;
		this.emailVerificationRepository = emailVerificationRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.authEmailSender = authEmailSender;
		this.auditService = auditService;
		this.appProperties = appProperties;
	}

	@Transactional
	public void register(RegisterRequest request, String ipAddress, String userAgent) {
		if (!request.getPassword().equals(request.getConfirmPassword())) {
			throw new BusinessException("Passwords do not match.");
		}

		String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new BusinessException("An account with this email address already exists.");
		}

		Role userRole = roleRepository.findByName(Role.ROLE_USER)
				.orElseThrow(() -> new IllegalStateException("Baseline ROLE_USER not found in database."));

		User user = new User();
		user.setFullName(request.getFullName().trim());
		user.setEmail(normalizedEmail);
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
		user.setEmailVerified(false);
		user.addRole(userRole);
		user = userRepository.save(user);

		UserProfile profile = new UserProfile();
		profile.setUser(user);
		profile.setDisplayName(user.getFullName());
		profile.setDefaultTimezone(resolveSafeTimezone(request.getTimezone()));
		profile.setLocale("en");
		userProfileRepository.save(profile);

		NotificationPreference preferences = new NotificationPreference();
		preferences.setUser(user);
		preferences.setEmailEnabled(true);
		preferences.setWhatsappEnabled(false);
		preferences.setUpcomingUnlockReminders(true);
		preferences.setOccasionReminders(true);
		notificationPreferenceRepository.save(preferences);

		String rawToken = issueVerificationToken(user);

		auditService.record(user.getId(), Role.ROLE_USER, "USER_REGISTERED", "USER", user.getPublicId(), ipAddress,
				userAgent, Map.of("email", normalizedEmail));

		String verifyUrl = appProperties.baseUrl() + "/auth/verify-email?token=" + rawToken;
		authEmailSender.sendVerificationEmail(user.getEmail(), user.getFullName(), verifyUrl);
	}

	@Transactional
	public void verifyEmail(String rawToken, String ipAddress, String userAgent) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new BusinessException("Verification token is missing.");
		}

		String tokenHash = TokenUtil.sha256Hex(rawToken.trim());
		Instant now = Instant.now();

		int updated = emailVerificationRepository.markVerifiedAtomically(tokenHash, now);
		if (updated != 1) {
			throw new BusinessException("This verification link is invalid, expired, or has already been used.");
		}

		EmailVerification verification = emailVerificationRepository.findByTokenHashWithUser(tokenHash)
				.orElseThrow(() -> new BusinessException("Verification record not found."));

		User user = verification.getUser();
		user.setEmailVerified(true);
		user.setEmailVerifiedAtUtc(now);
		if (user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
			user.setAccountStatus(AccountStatus.ACTIVE);
		}
		userRepository.save(user);

		auditService.record(user.getId(), Role.ROLE_USER, "EMAIL_VERIFIED", "USER", user.getPublicId(), ipAddress,
				userAgent, Map.of("email", user.getEmail()));
	}

	@Transactional
	public void resendVerification(String email, String ipAddress, String userAgent) {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
			if (!user.isEmailVerified() && user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
				emailVerificationRepository.deleteUnverifiedByUserId(user.getId());
				String rawToken = issueVerificationToken(user);

				auditService.record(user.getId(), Role.ROLE_USER, "EMAIL_VERIFICATION_RESENT", "USER",
						user.getPublicId(), ipAddress, userAgent, Map.of("email", normalizedEmail));

				String verifyUrl = appProperties.baseUrl() + "/auth/verify-email?token=" + rawToken;
				authEmailSender.sendVerificationEmail(user.getEmail(), user.getFullName(), verifyUrl);
			}
		});
	}

	@Transactional
	public void initiatePasswordReset(String email, String ipAddress, String userAgent) {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
			if (user.getAccountStatus() == AccountStatus.DELETED
					|| user.getAccountStatus() == AccountStatus.SUSPENDED) {
				return;
			}
			passwordResetTokenRepository.deleteUnusedByUserId(user.getId());

			String rawToken = TokenUtil.generateRawToken();
			PasswordResetToken resetToken = new PasswordResetToken();
			resetToken.setUser(user);
			resetToken.setTokenHash(TokenUtil.sha256Hex(rawToken));
			resetToken.setExpiresAtUtc(Instant.now().plus(PASSWORD_RESET_TTL));
			resetToken.setRequestIp(ipAddress);
			passwordResetTokenRepository.save(resetToken);

			auditService.record(user.getId(), Role.ROLE_USER, "PASSWORD_RESET_REQUESTED", "USER", user.getPublicId(),
					ipAddress, userAgent, Map.of("email", normalizedEmail));

			String resetUrl = appProperties.baseUrl() + "/auth/reset-password?token=" + rawToken;
			authEmailSender.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetUrl);
		});
	}

	@Transactional(readOnly = true)
	public boolean isResetTokenValid(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return false;
		}
		String tokenHash = TokenUtil.sha256Hex(rawToken.trim());
		Instant now = Instant.now();
		return passwordResetTokenRepository.findByTokenHashWithUser(tokenHash)
				.filter(token -> token.getUsedAtUtc() == null && token.getExpiresAtUtc().isAfter(now)).isPresent();
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request, String ipAddress, String userAgent) {
		if (!request.getNewPassword().equals(request.getConfirmPassword())) {
			throw new BusinessException("Passwords do not match.");
		}

		String tokenHash = TokenUtil.sha256Hex(request.getToken().trim());
		Instant now = Instant.now();

		int updated = passwordResetTokenRepository.markUsedAtomically(tokenHash, now);
		if (updated != 1) {
			throw new BusinessException("This password reset link is invalid, expired, or has already been used.");
		}

		PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashWithUser(tokenHash)
				.orElseThrow(() -> new BusinessException("Reset token not found."));

		User user = resetToken.getUser();
		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);

		auditService.record(user.getId(), Role.ROLE_USER, "PASSWORD_RESET_COMPLETED", "USER", user.getPublicId(),
				ipAddress, userAgent, Map.of("email", user.getEmail()));
	}

	private String issueVerificationToken(User user) {
		String rawToken = TokenUtil.generateRawToken();
		EmailVerification verification = new EmailVerification();
		verification.setUser(user);
		verification.setTokenHash(TokenUtil.sha256Hex(rawToken));
		verification.setExpiresAtUtc(Instant.now().plus(EMAIL_VERIFICATION_TTL));
		emailVerificationRepository.save(verification);
		return rawToken;
	}

	private String resolveSafeTimezone(String candidate) {
		if (candidate == null || candidate.isBlank()) {
			return "UTC";
		}
		try {
			return ZoneId.of(candidate.trim()).getId();
		} catch (Exception ex) {
			return "UTC";
		}
	}

}