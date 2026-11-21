package com.timefold.invitation.controller;

import com.timefold.auth.service.TimeFoldUserDetails;
import com.timefold.capsule.service.SharedCapsuleService;
import com.timefold.common.exception.BusinessException;
import com.timefold.common.util.RequestMetadataUtil;
import com.timefold.invitation.dto.PendingInvitationItem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/invitations")
public class InvitationController {

    private final SharedCapsuleService sharedCapsuleService;

    public InvitationController(SharedCapsuleService sharedCapsuleService) {
        this.sharedCapsuleService = sharedCapsuleService;
    }

    @GetMapping
    public String inbox(@AuthenticationPrincipal TimeFoldUserDetails principal, Model model) {
        model.addAttribute("invitations", sharedCapsuleService.listPendingInvitationsForUser(
                principal.getEmail(), principal.getUserId()));
        return "shared/invitations";
    }

    @GetMapping("/token")
    public String inspectByToken(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @RequestParam("token") String token,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            PendingInvitationItem item = sharedCapsuleService.inspectInvitationByToken(
                    token, principal.getEmail(), principal.getUserId());
            model.addAttribute("invitation", item);
            return "shared/invitation-review";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/invitations";
        }
    }

    @PostMapping("/{invitationId}/accept")
    public String accept(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable Long invitationId,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String capsulePublicId = sharedCapsuleService.acceptInvitation(
                    invitationId,
                    principal.getUserId(),
                    principal.getEmail(),
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "You have joined the shared capsule! Submit your message below before the owner seals it."
            );
            return "redirect:/capsules/" + capsulePublicId;
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/invitations";
        }
    }

    @PostMapping("/{invitationId}/decline")
    public String decline(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable Long invitationId,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            sharedCapsuleService.declineInvitation(
                    invitationId,
                    principal.getUserId(),
                    principal.getEmail(),
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Invitation declined.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/invitations";
    }

}