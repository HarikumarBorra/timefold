package com.timefold.user.controller;

import com.timefold.auth.service.TimeFoldUserDetails;
import com.timefold.common.exception.BusinessException;
import com.timefold.common.util.RequestMetadataUtil;
import com.timefold.user.dto.ChangePasswordRequest;
import com.timefold.user.dto.ProfileUpdateRequest;
import com.timefold.user.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.ZoneId;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

	private static final List<String> SORTED_TIMEZONES = ZoneId.getAvailableZoneIds().stream().sorted().toList();

	private final UserProfileService userProfileService;

	public ProfileController(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	@GetMapping
	public String profilePage(@AuthenticationPrincipal TimeFoldUserDetails principal, Model model) {
		model.addAttribute("profileUpdateRequest", userProfileService.loadProfileForm(principal.getUserId()));
		model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
		model.addAttribute("timezones", SORTED_TIMEZONES);
		model.addAttribute("userEmail", principal.getEmail());
		return "profile/index";
	}

	@PostMapping
	public String updateProfile(@AuthenticationPrincipal TimeFoldUserDetails principal,
			@Valid @ModelAttribute ProfileUpdateRequest profileUpdateRequest,
			BindingResult bindingResult, HttpServletRequest httpRequest, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
			model.addAttribute("timezones", SORTED_TIMEZONES);
			model.addAttribute("userEmail", principal.getEmail());
			return "profile/index";
		}
		try {
			userProfileService.updateProfile(principal.getUserId(), profileUpdateRequest,
					RequestMetadataUtil.extractClientIp(httpRequest),
					RequestMetadataUtil.extractUserAgent(httpRequest));
			redirectAttributes.addFlashAttribute("successMessage", "Profile and preferences updated.");
			return "redirect:/profile";
		} catch (BusinessException ex) {
			model.addAttribute("errorMessage", ex.getMessage());
			model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
			model.addAttribute("timezones", SORTED_TIMEZONES);
			model.addAttribute("userEmail", principal.getEmail());
			return "profile/index";
		}
	}

	@PostMapping("/password")
	public String changePassword(@AuthenticationPrincipal TimeFoldUserDetails principal,
			@Valid @ModelAttribute ChangePasswordRequest changePasswordRequest,
			BindingResult bindingResult, HttpServletRequest httpRequest, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("profileUpdateRequest", userProfileService.loadProfileForm(principal.getUserId()));
			model.addAttribute("timezones", SORTED_TIMEZONES);
			model.addAttribute("userEmail", principal.getEmail());
			return "profile/index";
		}
		try {
			userProfileService.changePassword(principal.getUserId(), changePasswordRequest,
					RequestMetadataUtil.extractClientIp(httpRequest),
					RequestMetadataUtil.extractUserAgent(httpRequest));
			redirectAttributes.addFlashAttribute("successMessage", "Your password has been changed.");
			return "redirect:/profile";
		} catch (BusinessException ex) {
			model.addAttribute("errorMessage", ex.getMessage());
			model.addAttribute("profileUpdateRequest", userProfileService.loadProfileForm(principal.getUserId()));
			model.addAttribute("timezones", SORTED_TIMEZONES);
			model.addAttribute("userEmail", principal.getEmail());
			return "profile/index";
		}
	}

}