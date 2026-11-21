package com.timefold.auth.service;

import com.timefold.audit.service.AuditService;
import com.timefold.user.repository.UserRepository;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LoginEventListener {

	private final UserRepository userRepository;
	private final AuditService auditService;

	public LoginEventListener(UserRepository userRepository, AuditService auditService) {
		this.userRepository = userRepository;
		this.auditService = auditService;
	}

	@EventListener
	@Transactional
	public void onLoginSuccess(AuthenticationSuccessEvent event) {
		if (event.getAuthentication().getPrincipal() instanceof TimeFoldUserDetails principal) {
			String ip = extractIp(event.getAuthentication().getDetails());
			userRepository.findById(principal.getUserId()).ifPresent(user -> {
				user.setLastLoginAtUtc(Instant.now());
				userRepository.save(user);
			});

			String primaryRole = principal.getAuthorities().stream().findFirst().map(Object::toString)
					.orElse("ROLE_USER");

			auditService.record(principal.getUserId(), primaryRole, "USER_LOGIN_SUCCESS", "USER",
					principal.getPublicId(), ip, null, Map.of("email", principal.getEmail()));
		}
	}

	@EventListener
	@Transactional
	public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
		String attemptedEmail = String.valueOf(event.getAuthentication().getPrincipal());
		String ip = extractIp(event.getAuthentication().getDetails());

		auditService.record(null, "ANONYMOUS", "USER_LOGIN_FAILED", "USER", attemptedEmail, ip, null,
				Map.of("reason", event.getException().getClass().getSimpleName()));
	}

	private String extractIp(Object details) {
		if (details instanceof WebAuthenticationDetails webDetails) {
			return webDetails.getRemoteAddress();
		}
		return null;
	}

}