package com.timefold.capsule.controller;

import com.timefold.auth.service.TimeFoldUserDetails;
import com.timefold.capsule.dto.CapsuleCardDto;
import com.timefold.capsule.dto.CapsuleDetailView;
import com.timefold.capsule.dto.PersonalCapsuleForm;
import com.timefold.capsule.service.PersonalCapsuleService;
import com.timefold.common.exception.BusinessException;
import com.timefold.common.util.RequestMetadataUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Page;
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
@RequestMapping("/capsules")
public class CapsuleController {

    private static final List<String> SORTED_TIMEZONES = ZoneId.getAvailableZoneIds().stream()
            .sorted()
            .toList();

    private static final List<String> AVAILABLE_THEMES = List.of(
            "CLASSIC", "NOSTALGIA", "CELEBRATION", "GRATITUDE", "MILESTONE"
    );

    private final PersonalCapsuleService personalCapsuleService;

    public CapsuleController(PersonalCapsuleService personalCapsuleService) {
        this.personalCapsuleService = personalCapsuleService;
    }

    @GetMapping
    public String listCapsules(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @RequestParam(name = "status", defaultValue = "ALL") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model
    ) {
        Page<CapsuleCardDto> capsulesPage = personalCapsuleService.listOwnerCapsules(
                principal.getUserId(), status, page, 9);
        model.addAttribute("capsulesPage", capsulesPage);
        model.addAttribute("activeFilter", status.toUpperCase());
        return "capsules/list";
    }

    @GetMapping("/new")
    public String newCapsuleForm(@AuthenticationPrincipal TimeFoldUserDetails principal, Model model) {
        model.addAttribute("capsuleForm", personalCapsuleService.prepareNewForm(principal.getUserId()));
        model.addAttribute("isEdit", false);
        populateFormLookups(model);
        return "capsules/form";
    }

    @PostMapping
    public String createCapsule(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @Valid @ModelAttribute("capsuleForm") PersonalCapsuleForm form,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            populateFormLookups(model);
            return "capsules/form";
        }
        try {
            String publicId = personalCapsuleService.createPersonalDraft(
                    principal.getUserId(),
                    form,
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Capsule saved as DRAFT. Review it below and seal it whenever you are ready."
            );
            return "redirect:/capsules/" + publicId;
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", false);
            populateFormLookups(model);
            return "capsules/form";
        }
    }

    @GetMapping("/{publicId}")
    public String capsuleDetail(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            Model model
    ) {
        CapsuleDetailView detail = personalCapsuleService.getCapsuleDetail(publicId, principal.getUserId());
        model.addAttribute("capsule", detail);

        if (detail.isShared()) {
            model.addAttribute("inviteForm", new com.timefold.invitation.dto.InviteContributorForm());
            com.timefold.capsule.dto.ContributionForm contributionForm = new com.timefold.capsule.dto.ContributionForm();
            contributionForm.setTitle(detail.ownContributionTitle());
            contributionForm.setMessageContent(detail.ownContributionMessage());
            model.addAttribute("contributionForm", contributionForm);
            model.addAttribute("visibilityPolicies", com.timefold.common.enums.VisibilityPolicy.values());
        }
        return "capsules/detail";
    }

    @GetMapping("/{publicId}/edit")
    public String editCapsuleForm(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            PersonalCapsuleForm form = personalCapsuleService.loadDraftForEdit(publicId, principal.getUserId());
            model.addAttribute("capsuleForm", form);
            model.addAttribute("isEdit", true);
            model.addAttribute("publicId", publicId);
            populateFormLookups(model);
            return "capsules/form";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/capsules/" + publicId;
        }
    }

    @PostMapping("/{publicId}/edit")
    public String updateCapsule(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            @Valid @ModelAttribute("capsuleForm") PersonalCapsuleForm form,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("publicId", publicId);
            populateFormLookups(model);
            return "capsules/form";
        }
        try {
            personalCapsuleService.updatePersonalDraft(
                    publicId,
                    principal.getUserId(),
                    form,
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Draft updated.");
            return "redirect:/capsules/" + publicId;
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("publicId", publicId);
            populateFormLookups(model);
            return "capsules/form";
        }
    }

    @PostMapping("/{publicId}/seal")
    public String sealCapsule(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            personalCapsuleService.sealCapsule(
                    publicId,
                    principal.getUserId(),
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Capsule sealed! Your message is safely locked until its scheduled unlock time."
            );
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/capsules/" + publicId;
    }

    @PostMapping("/{publicId}/open")
    public String openCapsule(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            personalCapsuleService.openCapsule(
                    publicId,
                    principal.getUserId(),
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Welcome back to your memory!");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/capsules/" + publicId;
    }

    @PostMapping("/{publicId}/cancel")
    public String cancelCapsule(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            personalCapsuleService.cancelSealedCapsule(
                    publicId,
                    principal.getUserId(),
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Sealed capsule has been cancelled.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/capsules/" + publicId;
    }

    @PostMapping("/{publicId}/delete")
    public String deleteDraft(
            @AuthenticationPrincipal TimeFoldUserDetails principal,
            @PathVariable String publicId,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            personalCapsuleService.deleteDraftCapsule(
                    publicId,
                    principal.getUserId(),
                    RequestMetadataUtil.extractClientIp(httpRequest),
                    RequestMetadataUtil.extractUserAgent(httpRequest)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Draft capsule deleted.");
            return "redirect:/capsules";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/capsules/" + publicId;
        }
    }

    private void populateFormLookups(Model model) {
        model.addAttribute("timezones", SORTED_TIMEZONES);
        model.addAttribute("themes", AVAILABLE_THEMES);
    }

}