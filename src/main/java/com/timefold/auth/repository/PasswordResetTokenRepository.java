package com.timefold.auth.repository;

import com.timefold.auth.entity.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	@Query("SELECT prt FROM PasswordResetToken prt JOIN FETCH prt.user WHERE prt.tokenHash = :tokenHash")
	Optional<PasswordResetToken> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);

	@Modifying
	@Query("DELETE FROM PasswordResetToken prt WHERE prt.user.id = :userId AND prt.usedAtUtc IS NULL")
	void deleteUnusedByUserId(@Param("userId") Long userId);

	@Modifying
	@Query("UPDATE PasswordResetToken prt SET prt.usedAtUtc = :now "
			+ "WHERE prt.tokenHash = :tokenHash AND prt.usedAtUtc IS NULL AND prt.expiresAtUtc > :now")
	int markUsedAtomically(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

}