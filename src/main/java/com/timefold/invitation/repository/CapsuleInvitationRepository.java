package com.timefold.invitation.repository;

import com.timefold.common.enums.CapsuleInvitationStatus;
import com.timefold.invitation.entity.CapsuleInvitation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CapsuleInvitationRepository extends JpaRepository<CapsuleInvitation, Long> {

    @Query("SELECT ci FROM CapsuleInvitation ci "
            + "JOIN FETCH ci.capsule c "
            + "JOIN FETCH c.owner "
            + "JOIN FETCH ci.invitedBy "
            + "WHERE ci.tokenHash = :tokenHash")
    Optional<CapsuleInvitation> findByTokenHashWithDetails(@Param("tokenHash") String tokenHash);

    @Query("SELECT ci FROM CapsuleInvitation ci "
            + "JOIN FETCH ci.capsule c "
            + "JOIN FETCH c.owner "
            + "JOIN FETCH ci.invitedBy "
            + "WHERE ci.id = :id")
    Optional<CapsuleInvitation> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT ci FROM CapsuleInvitation ci "
            + "JOIN FETCH ci.invitedBy "
            + "WHERE ci.capsule.id = :capsuleId "
            + "ORDER BY ci.createdAtUtc DESC")
    List<CapsuleInvitation> findByCapsuleIdOrderByCreatedAtDesc(@Param("capsuleId") Long capsuleId);

    boolean existsByCapsuleIdAndInviteeEmailIgnoreCaseAndStatusAndExpiresAtUtcAfter(
            Long capsuleId,
            String inviteeEmail,
            CapsuleInvitationStatus status,
            Instant now
    );

    @Query("SELECT ci FROM CapsuleInvitation ci "
            + "JOIN FETCH ci.capsule c "
            + "JOIN FETCH ci.invitedBy "
            + "WHERE LOWER(ci.inviteeEmail) = LOWER(:email) "
            + "AND ci.status = com.timefold.common.enums.CapsuleInvitationStatus.PENDING "
            + "AND ci.expiresAtUtc > :now "
            + "ORDER BY ci.createdAtUtc DESC")
    List<CapsuleInvitation> findActivePendingForEmail(@Param("email") String email, @Param("now") Instant now);

    @Query("SELECT COUNT(ci) FROM CapsuleInvitation ci "
            + "WHERE LOWER(ci.inviteeEmail) = LOWER(:email) "
            + "AND ci.status = com.timefold.common.enums.CapsuleInvitationStatus.PENDING "
            + "AND ci.expiresAtUtc > :now")
    long countActivePendingForEmail(@Param("email") String email, @Param("now") Instant now);

    long countByInvitedByIdAndCreatedAtUtcAfter(Long invitedByUserId, Instant sinceUtc);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CapsuleInvitation ci SET ci.status = :newStatus, ci.respondedAtUtc = :now "
            + "WHERE ci.id = :id "
            + "AND ci.status = com.timefold.common.enums.CapsuleInvitationStatus.PENDING "
            + "AND ci.expiresAtUtc > :now")
    int transitionPendingAtomically(
            @Param("id") Long id,
            @Param("newStatus") CapsuleInvitationStatus newStatus,
            @Param("now") Instant now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CapsuleInvitation ci SET ci.status = com.timefold.common.enums.CapsuleInvitationStatus.REVOKED, "
            + "ci.respondedAtUtc = :now "
            + "WHERE ci.capsule.id = :capsuleId "
            + "AND ci.status = com.timefold.common.enums.CapsuleInvitationStatus.PENDING")
    int revokeAllPendingForCapsule(@Param("capsuleId") Long capsuleId, @Param("now") Instant now);
    
}