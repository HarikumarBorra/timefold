package com.timefold.auth.controller;

import com.timefold.auth.dto.EmailRequest;
import com.timefold.auth.dto.RegisterRequest;
import com.timefold.auth.dto.ResetPasswordRequest;
import com.timefold.auth.service.AuthService;
import com.timefold.common.exception.BusinessException;
import com.timefold.common.util.RequestMetadataUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/login")
	public String loginPage(Authentication authentication) {
		if (isAuthenticated(authentication)) {
			return "redirect:/dashboard";
		}
		return "auth/login";
	}

	@GetMapping("/register")
	public String registerPage(Authentication authentication, Model model) {
		if (isAuthenticated(authentication)) {
			return "redirect:/dashboard";
		}
		model.addAttribute("registerRequest", new RegisterRequest());
		return "auth/register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute RegisterRequest registerRequest,
			BindingResult bindingResult, HttpServletRequest httpRequest, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return "auth/register";
		}

		try {
			authService.register(registerRequest, RequestMetadataUtil.extractClientIp(httpRequest),
					RequestMetadataUtil.extractUserAgent(httpRequest));
			redirectAttributes.addFlashAttribute("successMessage",
					"Account created! Check your email (or STS console in dev mode) to verify your account before logging in.");
			return "redirect:/auth/login";
		} catch (BusinessException ex) {
			model.addAttribute("errorMessage", ex.getMessage());
			return "auth/register";
		}
	}

	@GetMapping("/verify-email")
	public String verifyEmail(@RequestParam String token, HttpServletRequest httpRequest,
			RedirectAttributes redirectAttributes) {
		try {
			authService.verifyEmail(token, RequestMetadataUtil.extractClientIp(httpRequest),
					RequestMetadataUtil.extractUserAgent(httpRequest));
			redirectAttributes.addFlashAttribute("successMessage",
					"Your email is verified! You can now log in to TimeFold.");
		} catch (BusinessException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/auth/login";
	}

	@GetMapping("/resend-verification")
	public String resendVerificationPage(Model model) {
		model.addAttribute("emailRequest", new EmailRequest());
		return "auth/resend-verification";
	}

	@PostMapping("/resend-verification")
	public String resendVerification(@Valid @ModelAttribute EmailRequest emailRequest,
			BindingResult bindingResult, HttpServletRequest httpRequest, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return "auth/resend-verification";
		}
		authService.resendVerification(emailRequest.getEmail(), RequestMetadataUtil.extractClientIp(httpRequest),
				RequestMetadataUtil.extractUserAgent(httpRequest));
		redirectAttributes.addFlashAttribute("successMessage",
				"If an unverified account exists for that email, a fresh verification link has been sent.");
		return "redirect:/auth/login";
	}

	@GetMapping("/forgot-password")
	public String forgotPasswordPage(Model model) {
		model.addAttribute("emailRequest", new EmailRequest());
		return "auth/forgot-password";
	}

	@PostMapping("/forgot-password")
	public String forgotPassword(@Valid @ModelAttribute EmailRequest emailRequest,
			BindingResult bindingResult, HttpServletRequest httpRequest, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return "auth/forgot-password";
		}
		authService.initiatePasswordReset(emailRequest.getEmail(), RequestMetadataUtil.extractClientIp(httpRequest),
				RequestMetadataUtil.extractUserAgent(httpRequest));
		redirectAttributes.addFlashAttribute("successMessage",
				"If an active account matches that email, a password reset link has been sent.");
		return "redirect:/auth/login";
	}

	@GetMapping("/reset-password")
	public String resetPasswordPage(@RequestParam String token, Model model,
			RedirectAttributes redirectAttributes) {
		if (!authService.isResetTokenValid(token)) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"This password reset link is invalid or has expired. Please request a new one.");
			return "redirect:/auth/forgot-password";
		}
		ResetPasswordRequest form = new ResetPasswordRequest();
		form.setToken(token);
		model.addAttribute("resetPasswordRequest", form);
		return "auth/reset-password";
	}

	@PostMapping("/reset-password")
	public String resetPassword(
			@Valid @ModelAttribute ResetPasswordRequest resetPasswordRequest,
			BindingResult bindingResult, HttpServletRequest httpRequest, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return "auth/reset-password";
		}
		try {
			authService.resetPassword(resetPasswordRequest, RequestMetadataUtil.extractClientIp(httpRequest),
					RequestMetadataUtil.extractUserAgent(httpRequest));
			redirectAttributes.addFlashAttribute("successMessage",
					"Password updated successfully. Please log in with your new password.");
			return "redirect:/auth/login";
		} catch (BusinessException ex) {
			model.addAttribute("errorMessage", ex.getMessage());
			return "auth/reset-password";
		}
	}

	private boolean isAuthenticated(Authentication authentication) {
		return authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}

}