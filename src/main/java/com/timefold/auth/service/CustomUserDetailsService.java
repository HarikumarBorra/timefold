package com.timefold.auth.service;

import com.timefold.user.entity.User;
import com.timefold.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (username == null || username.isBlank()) {
			throw new UsernameNotFoundException("Email is required");
		}
		User user = userRepository.findByEmailWithRoles(username.trim())
				.orElseThrow(() -> new UsernameNotFoundException("No account found for email"));
		return new TimeFoldUserDetails(user);
	}

}