package com.timefold.user.repository;

import com.timefold.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

	boolean existsByPhoneE164AndUserIdNot(String phoneE164, Long userId);

}