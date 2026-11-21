package com.timefold.capsule.controller;

import com.timefold.auth.service.TimeFoldUserDetails;
import com.timefold.capsule.dto.ContributionForm;
import com.timefold.capsule.dto.SharedCapsuleForm;
import com.timefold.capsule.service.SharedCapsuleService;
import com.timefold.common.enums.VisibilityPolicy;
import com.timefold.common.exception.BusinessException;
import com.timefold.common.util.RequestMetadataUtil;
import com.timefold.invitation.dto.InviteContributorForm;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shared")
public class SharedCapsuleController {

    private static final List<String> SORTED_TIMEZONES = ZoneId.getAvailableZoneIds().stream()
            .sorted()
            .toList();

    private static final List<String> AVAILABLE_THEMES = List.of(
            "CELEBRATION", "CLASSIC", "NOSTALGIA", "GRATITUDE", "MILESTONE"
    );

    private final SharedCapsuleService sharedCapsuleService;

    public SharedCapsuleController(SharedCapsuleService sharedCapsuleService) {
        this.sharedCapsuleService = sharedCapsuleService;
    }

    @GetMapping("/new")
    public String newSharedCapsuleForm(@AuthenticationPrincipal TimeFoldUserDetails principal, Model model) {
        model.addAttribute("sharedForm", sharedCapsuleService.prepareNewSharedForm(principal.getUserId()));
        populateLookups(model);
        return "shared/form";
    }

    @PostMapping
    public String createSharedCapsule(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @Valid @ModelAttribute("sharedForm") SharedCapsuleForm form,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateLookups(model);
            return "shared/form";
        }
        try {
            String publicId = sharedCapsuleService.createSharedCapsule(
                    principal.getUserId(),
                    form,
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Shared capsule created! Invite contributors and configure visibility before sealing."
            );
            return "redirect:/capsules/" + publicId;
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateLookups(model);
            return "shared/form";
        }
    }

    @PostMapping("/{publicId}/invite")
    public String inviteContributor(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            @Valid @ModelAttribute("inviteForm") InviteContributorForm inviteForm,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please enter a valid email address to invite.");
            return "redirect:/capsules/" + publicId;
        }
        try {
            sharedCapsuleService.inviteContributor(
                    publicId,
                    principal.getUserId(),
                    inviteForm.getInviteeEmail(),
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Invitation sent to " + inviteForm.getInviteeEmail().trim()
                            + " (check your STS console in dev mode for the invite link)."
            );
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/capsules/" + publicId;
    }

    @PostMapping("/{publicId}/invitations/{invitationId}/revoke")
    public String revokeInvitation(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            @PathVariable Long invitationId,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            sharedCapsuleService.revokeInvitation(
                    publicId,
                    invitationId,
                    principal.getUserId(),
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Invitation revoked.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/capsules/" + publicId;
    }

    @PostMapping("/{publicId}/contributions")
    public String saveContribution(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            @Valid @ModelAttribute("contributionForm") ContributionForm contributionForm,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your contribution message cannot be empty.");
            return "redirect:/capsules/" + publicId;
        }
        try {
            sharedCapsuleService.saveContribution(
                    publicId,
                    principal.getUserId(),
                    contributionForm,
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Your contribution has been saved and encrypted.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/capsules/" + publicId;
    }

    @PostMapping("/{publicId}/visibility-policy")
    public String updateVisibilityPolicy(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            @RequestParam("visibilityPolicy") VisibilityPolicy visibilityPolicy,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            sharedCapsuleService.updateSharedSettings(
                    publicId,
                    principal.getUserId(),
                    visibilityPolicy,
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Visibility policy updated to " + visibilityPolicy + ".");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/capsules/" + publicId;
    }

    @PostMapping("/{publicId}/members/{memberId}/toggle-edit")
    public String toggleMemberEdit(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            @PathVariable Long memberId,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            sharedCapsuleService.toggleContributorEditPermission(
                    publicId,
                    memberId,
                    principal.getUserId(),
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Contributor edit permission updated.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/capsules/" + publicId;
    }

    @PostMapping("/{publicId}/members/{memberId}/remove")
    public String removeMember(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            @PathVariable Long memberId,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            sharedCapsuleService.removeContributor(
                    publicId,
                    memberId,
                    principal.getUserId(),
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Contributor removed from capsule.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/capsules/" + publicId;
    }

    @PostMapping("/{publicId}/contributions/{contributionPublicId}/visibility")
    public String updateCustomVisibility(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            @PathVariable String contributionPublicId,
            @RequestParam(name = "allowedUserIds", required = false) List<Long> allowedUserIds,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            sharedCapsuleService.updateContributionVisibilityAllowList(
                    publicId,
                    contributionPublicId,
                    allowedUserIds,
                    principal.getUserId(),
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Custom visibility allow-list saved.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/capsules/" + publicId;
    }

    private void populateLookups(Model model) {
        model.addAttribute("timezones", SORTED_TIMEZONES);
        model.addAttribute("themes", AVAILABLE_THEMES);
        model.addAttribute("visibilityPolicies", VisibilityPolicy.values());
    }

}