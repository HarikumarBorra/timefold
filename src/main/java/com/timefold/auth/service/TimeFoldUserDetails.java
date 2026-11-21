package com.timefold.auth.service;

import com.timefold.common.enums.AccountStatus;
import com.timefold.user.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class TimeFoldUserDetails implements UserDetails {

	private static final long serialVersionUID = 1509530189656222327L;

	private final Long userId;
	private final String publicId;
	private final String fullName;
	private final String email;
	private final String passwordHash;
	private final AccountStatus accountStatus;
	private final boolean emailVerified;
	private final List<SimpleGrantedAuthority> authorities;

	public TimeFoldUserDetails(User user) {
		this.userId = user.getId();
		this.publicId = user.getPublicId();
		this.fullName = user.getFullName();
		this.email = user.getEmail();
		this.passwordHash = user.getPasswordHash();
		this.accountStatus = user.getAccountStatus();
		this.emailVerified = user.isEmailVerified();
		this.authorities = user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList();
	}

	public Long getUserId() {
		return userId;
	}

	public String getPublicId() {
		return publicId;
	}

	public String getFullName() {
		return fullName;
	}

	public String getEmail() {
		return email;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return accountStatus != AccountStatus.DELETED;
	}

	@Override
	public boolean isAccountNonLocked() {
		return accountStatus != AccountStatus.SUSPENDED;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return emailVerified && accountStatus == AccountStatus.ACTIVE;
	}

}