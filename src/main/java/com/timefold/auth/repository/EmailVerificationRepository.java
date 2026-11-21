package com.timefold.auth.repository;

import com.timefold.auth.entity.EmailVerification;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

	@Query("SELECT ev FROM EmailVerification ev JOIN FETCH ev.user WHERE ev.tokenHash = :tokenHash")
	Optional<EmailVerification> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);

	@Modifying
	@Query("DELETE FROM EmailVerification ev WHERE ev.user.id = :userId AND ev.verifiedAtUtc IS NULL")
	void deleteUnverifiedByUserId(@Param("userId") Long userId);

	@Modifying
	@Query("UPDATE EmailVerification ev SET ev.verifiedAtUtc = :now "
			+ "WHERE ev.tokenHash = :tokenHash AND ev.verifiedAtUtc IS NULL AND ev.expiresAtUtc > :now")
	int markVerifiedAtomically(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

}