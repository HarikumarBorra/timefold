package com.timefold.user.controller;

import com.timefold.auth.service.TimeFoldUserDetails;
import com.timefold.capsule.service.PersonalCapsuleService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

	private final PersonalCapsuleService personalCapsuleService;

	public DashboardController(PersonalCapsuleService personalCapsuleService) {
		this.personalCapsuleService = personalCapsuleService;
	}

	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal TimeFoldUserDetails principal, Model model) {
		Long userId = principal.getUserId();
		model.addAttribute("summary", personalCapsuleService.buildLiveDashboardSummary(userId));
		model.addAttribute("readyCapsules",
				personalCapsuleService.listAccessibleCapsules(userId, "READY", 0, 4).getContent());
		model.addAttribute("recentCapsules",
				personalCapsuleService.listAccessibleCapsules(userId, "ALL", 0, 6).getContent());
		return "dashboard/index";
	}

}