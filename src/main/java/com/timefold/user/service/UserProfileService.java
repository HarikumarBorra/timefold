package com.timefold.user.service;

import com.timefold.audit.service.AuditService;
import com.timefold.common.exception.BusinessException;
import com.timefold.notification.entity.NotificationPreference;
import com.timefold.notification.repository.NotificationPreferenceRepository;
import com.timefold.user.dto.ChangePasswordRequest;
import com.timefold.user.dto.ProfileUpdateRequest;
import com.timefold.user.dto.UserDashboardSummary;
import com.timefold.user.entity.Role;
import com.timefold.user.entity.User;
import com.timefold.user.entity.UserProfile;
import com.timefold.user.repository.UserProfileRepository;
import com.timefold.user.repository.UserRepository;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

	private final UserRepository userRepository;
	private final UserProfileRepository userProfileRepository;
	private final NotificationPreferenceRepository notificationPreferenceRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditService auditService;

	public UserProfileService(UserRepository userRepository, UserProfileRepository userProfileRepository,
			NotificationPreferenceRepository notificationPreferenceRepository, PasswordEncoder passwordEncoder,
			AuditService auditService) {
		this.userRepository = userRepository;
		this.userProfileRepository = userProfileRepository;
		this.notificationPreferenceRepository = notificationPreferenceRepository;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
	}

	@Transactional(readOnly = true)
	public ProfileUpdateRequest loadProfileForm(Long userId) {
		User user = findUser(userId);
		UserProfile profile = userProfileRepository.findById(userId).orElse(null);
		NotificationPreference prefs = notificationPreferenceRepository.findById(userId).orElse(null);

		ProfileUpdateRequest form = new ProfileUpdateRequest();
		form.setFullName(user.getFullName());
		if (profile != null) {
			form.setDisplayName(profile.getDisplayName());
			form.setPhoneE164(profile.getPhoneE164());
			form.setDefaultTimezone(profile.getDefaultTimezone());
			form.setBio(profile.getBio());
		}
		if (prefs != null) {
			form.setEmailEnabled(prefs.isEmailEnabled());
			form.setUpcomingUnlockReminders(prefs.isUpcomingUnlockReminders());
			form.setOccasionReminders(prefs.isOccasionReminders());
		}
		return form;
	}

	@Transactional
	public void updateProfile(Long userId, ProfileUpdateRequest request, String ipAddress, String userAgent) {
		User user = findUser(userId);
		String validatedTimezone = validateTimezone(request.getDefaultTimezone());

		String normalizedPhone = (request.getPhoneE164() == null || request.getPhoneE164().isBlank()) ? null
				: request.getPhoneE164().trim();

		if (normalizedPhone != null && userProfileRepository.existsByPhoneE164AndUserIdNot(normalizedPhone, userId)) {
			throw new BusinessException("That phone number is already linked to another account.");
		}

		user.setFullName(request.getFullName().trim());
		userRepository.save(user);

		UserProfile profile = userProfileRepository.findById(userId).orElseGet(() -> {
			UserProfile created = new UserProfile();
			created.setUser(user);
			return created;
		});

		String oldPhone = profile.getPhoneE164();
		profile.setDisplayName(request.getDisplayName() != null && !request.getDisplayName().isBlank()
				? request.getDisplayName().trim()
				: user.getFullName());
		profile.setPhoneE164(normalizedPhone);
		if (!Objects.equals(oldPhone, normalizedPhone)) {
			profile.setPhoneVerified(false);
			profile.setPhoneVerifiedAtUtc(null);
		}
		profile.setDefaultTimezone(validatedTimezone);
		profile.setBio(request.getBio() != null ? request.getBio().trim() : null);
		userProfileRepository.save(profile);

		NotificationPreference prefs = notificationPreferenceRepository.findById(userId).orElseGet(() -> {
			NotificationPreference created = new NotificationPreference();
			created.setUser(user);
			return created;
		});
		prefs.setEmailEnabled(request.isEmailEnabled());
		prefs.setUpcomingUnlockReminders(request.isUpcomingUnlockReminders());
		prefs.setOccasionReminders(request.isOccasionReminders());
		notificationPreferenceRepository.save(prefs);

		auditService.record(user.getId(), Role.ROLE_USER, "PROFILE_UPDATED", "USER", user.getPublicId(), ipAddress,
				userAgent, Map.of("timezone", validatedTimezone));
	}

	@Transactional
	public void changePassword(Long userId, ChangePasswordRequest request, String ipAddress, String userAgent) {
		if (!request.getNewPassword().equals(request.getConfirmPassword())) {
			throw new BusinessException("New passwords do not match.");
		}

		User user = findUser(userId);
		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
			throw new BusinessException("Current password is incorrect.");
		}

		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);

		auditService.record(user.getId(), Role.ROLE_USER, "PASSWORD_CHANGED", "USER", user.getPublicId(), ipAddress,
				userAgent, Map.of());
	}

	@Transactional(readOnly = true)
	public UserDashboardSummary getDashboardSummary(Long userId) {
		User user = findUser(userId);
		UserProfile profile = userProfileRepository.findById(userId).orElse(null);

		String displayName = (profile != null && profile.getDisplayName() != null
				&& !profile.getDisplayName().isBlank()) ? profile.getDisplayName() : user.getFullName();
		String timezone = (profile != null && profile.getDefaultTimezone() != null) ? profile.getDefaultTimezone()
				: "UTC";

		// Capsule counts will be wired to live queries in Phases 3–6
		return new UserDashboardSummary(displayName, user.getEmail(), timezone, 0, 0, 0, 0, 0, 0, 0);
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new BusinessException("User account not found."));
	}

	private String validateTimezone(String timezone) {
		try {
			return ZoneId.of(timezone.trim()).getId();
		} catch (Exception ex) {
			throw new BusinessException("Invalid timezone selected.");
		}
	}

}