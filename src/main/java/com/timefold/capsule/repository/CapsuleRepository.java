package com.timefold.capsule.repository;

import com.timefold.capsule.entity.Capsule;
import com.timefold.common.enums.CapsuleStatus;
import com.timefold.common.enums.CapsuleType;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CapsuleRepository extends JpaRepository<Capsule, Long> {

    @Query("SELECT c FROM Capsule c JOIN FETCH c.owner WHERE c.publicId = :publicId")
    Optional<Capsule> findByPublicIdWithOwner(@Param("publicId") String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Capsule c JOIN FETCH c.owner WHERE c.publicId = :publicId")
    Optional<Capsule> findByPublicIdForUpdate(@Param("publicId") String publicId);

    // ---- Member-aware queries (Owner OR Accepted CapsuleMember) ----

    @Query("SELECT c FROM Capsule c WHERE c.owner.id = :userId "
            + "OR EXISTS (SELECT cm FROM CapsuleMember cm WHERE cm.capsule.id = c.id "
            + "AND cm.user.id = :userId AND cm.invitationStatus = com.timefold.common.enums.InvitationStatus.ACCEPTED) "
            + "ORDER BY c.updatedAtUtc DESC")
    Page<Capsule> findAccessibleByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Capsule c WHERE c.capsuleType = :type AND (c.owner.id = :userId "
            + "OR EXISTS (SELECT cm FROM CapsuleMember cm WHERE cm.capsule.id = c.id "
            + "AND cm.user.id = :userId AND cm.invitationStatus = com.timefold.common.enums.InvitationStatus.ACCEPTED)) "
            + "ORDER BY c.updatedAtUtc DESC")
    Page<Capsule> findAccessibleByUserAndType(
            @Param("userId") Long userId,
            @Param("type") CapsuleType type,
            Pageable pageable
    );

    @Query("SELECT c FROM Capsule c WHERE c.status = :status AND (c.owner.id = :userId "
            + "OR EXISTS (SELECT cm FROM CapsuleMember cm WHERE cm.capsule.id = c.id "
            + "AND cm.user.id = :userId AND cm.invitationStatus = com.timefold.common.enums.InvitationStatus.ACCEPTED)) "
            + "ORDER BY c.updatedAtUtc DESC")
    Page<Capsule> findAccessibleByUserAndStatus(
            @Param("userId") Long userId,
            @Param("status") CapsuleStatus status,
            Pageable pageable
    );

    @Query("SELECT c FROM Capsule c WHERE (c.owner.id = :userId "
            + "OR EXISTS (SELECT cm FROM CapsuleMember cm WHERE cm.capsule.id = c.id "
            + "AND cm.user.id = :userId AND cm.invitationStatus = com.timefold.common.enums.InvitationStatus.ACCEPTED)) "
            + "AND c.status IN (com.timefold.common.enums.CapsuleStatus.READY, com.timefold.common.enums.CapsuleStatus.OPENED) "
            + "AND NOT EXISTS (SELECT co FROM CapsuleOpen co WHERE co.id.capsuleId = c.id AND co.id.userId = :userId) "
            + "ORDER BY c.unlockAtUtc ASC")
    Page<Capsule> findReadyForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Capsule c WHERE (c.owner.id = :userId "
            + "OR EXISTS (SELECT cm FROM CapsuleMember cm WHERE cm.capsule.id = c.id "
            + "AND cm.user.id = :userId AND cm.invitationStatus = com.timefold.common.enums.InvitationStatus.ACCEPTED)) "
            + "AND c.status IN (com.timefold.common.enums.CapsuleStatus.READY, com.timefold.common.enums.CapsuleStatus.OPENED) "
            + "AND EXISTS (SELECT co FROM CapsuleOpen co WHERE co.id.capsuleId = c.id AND co.id.userId = :userId) "
            + "ORDER BY c.updatedAtUtc DESC")
    Page<Capsule> findOpenedForUser(@Param("userId") Long userId, Pageable pageable);

    // ---- Member-aware counts ----

    long countByOwnerIdAndCapsuleType(Long ownerId, CapsuleType capsuleType);

    @Query("SELECT COUNT(c) FROM Capsule c WHERE c.status = :status AND (c.owner.id = :userId "
            + "OR EXISTS (SELECT cm FROM CapsuleMember cm WHERE cm.capsule.id = c.id "
            + "AND cm.user.id = :userId AND cm.invitationStatus = com.timefold.common.enums.InvitationStatus.ACCEPTED))")
    long countAccessibleByUserAndStatus(@Param("userId") Long userId, @Param("status") CapsuleStatus status);

    @Query("SELECT COUNT(c) FROM Capsule c WHERE (c.owner.id = :userId "
            + "OR EXISTS (SELECT cm FROM CapsuleMember cm WHERE cm.capsule.id = c.id "
            + "AND cm.user.id = :userId AND cm.invitationStatus = com.timefold.common.enums.InvitationStatus.ACCEPTED)) "
            + "AND c.status IN (com.timefold.common.enums.CapsuleStatus.READY, com.timefold.common.enums.CapsuleStatus.OPENED) "
            + "AND NOT EXISTS (SELECT co FROM CapsuleOpen co WHERE co.id.capsuleId = c.id AND co.id.userId = :userId)")
    long countReadyForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM Capsule c WHERE (c.owner.id = :userId "
            + "OR EXISTS (SELECT cm FROM CapsuleMember cm WHERE cm.capsule.id = c.id "
            + "AND cm.user.id = :userId AND cm.invitationStatus = com.timefold.common.enums.InvitationStatus.ACCEPTED)) "
            + "AND c.status IN (com.timefold.common.enums.CapsuleStatus.READY, com.timefold.common.enums.CapsuleStatus.OPENED) "
            + "AND EXISTS (SELECT co FROM CapsuleOpen co WHERE co.id.capsuleId = c.id AND co.id.userId = :userId)")
    long countOpenedForUser(@Param("userId") Long userId);

    // ---- Legacy owner-only helpers (kept for backward compatibility during Phase 4 transition) ----

    Page<Capsule> findByOwnerIdOrderByUpdatedAtUtcDesc(Long ownerId, Pageable pageable);

    Page<Capsule> findByOwnerIdAndStatusOrderByUpdatedAtUtcDesc(Long ownerId, CapsuleStatus status, Pageable pageable);

    @Query("SELECT c FROM Capsule c WHERE c.owner.id = :ownerId "
            + "AND c.status IN (com.timefold.common.enums.CapsuleStatus.READY, com.timefold.common.enums.CapsuleStatus.OPENED) "
            + "AND NOT EXISTS (SELECT co FROM CapsuleOpen co WHERE co.id.capsuleId = c.id AND co.id.userId = :ownerId) "
            + "ORDER BY c.unlockAtUtc ASC")
    Page<Capsule> findReadyForOwner(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("SELECT c FROM Capsule c WHERE c.owner.id = :ownerId "
            + "AND c.status IN (com.timefold.common.enums.CapsuleStatus.READY, com.timefold.common.enums.CapsuleStatus.OPENED) "
            + "AND EXISTS (SELECT co FROM CapsuleOpen co WHERE co.id.capsuleId = c.id AND co.id.userId = :ownerId) "
            + "ORDER BY c.updatedAtUtc DESC")
    Page<Capsule> findOpenedForOwner(@Param("ownerId") Long ownerId, Pageable pageable);

    long countByOwnerId(Long ownerId);

    long countByOwnerIdAndStatus(Long ownerId, CapsuleStatus status);

    @Query("SELECT COUNT(c) FROM Capsule c WHERE c.owner.id = :ownerId "
            + "AND c.status IN (com.timefold.common.enums.CapsuleStatus.READY, com.timefold.common.enums.CapsuleStatus.OPENED) "
            + "AND NOT EXISTS (SELECT co FROM CapsuleOpen co WHERE co.id.capsuleId = c.id AND co.id.userId = :ownerId)")
    long countReadyForOwner(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(c) FROM Capsule c WHERE c.owner.id = :ownerId "
            + "AND c.status IN (com.timefold.common.enums.CapsuleStatus.READY, com.timefold.common.enums.CapsuleStatus.OPENED) "
            + "AND EXISTS (SELECT co FROM CapsuleOpen co WHERE co.id.capsuleId = c.id AND co.id.userId = :ownerId)")
    long countOpenedForOwner(@Param("ownerId") Long ownerId);

    // ---- Scheduler & Atomic Lifecycle Transitions ----

    @Query("SELECT c.id FROM Capsule c WHERE c.status = com.timefold.common.enums.CapsuleStatus.SEALED "
            + "AND c.unlockAtUtc <= :now ORDER BY c.unlockAtUtc ASC")
    List<Long> findDueSealedCapsuleIds(@Param("now") Instant now, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Capsule c SET c.status = com.timefold.common.enums.CapsuleStatus.READY, "
            + "c.unlockedAtUtc = :now, c.updatedAtUtc = :now "
            + "WHERE c.id = :capsuleId "
            + "AND c.status = com.timefold.common.enums.CapsuleStatus.SEALED "
            + "AND c.unlockAtUtc <= :now")
    int unlockSealedCapsuleAtomically(@Param("capsuleId") Long capsuleId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Capsule c SET c.status = com.timefold.common.enums.CapsuleStatus.OPENED, "
            + "c.openedAtUtc = :now, c.updatedAtUtc = :now "
            + "WHERE c.id = :capsuleId "
            + "AND c.status = com.timefold.common.enums.CapsuleStatus.READY")
    int markReadyCapsuleOpenedAtomically(@Param("capsuleId") Long capsuleId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Capsule c SET c.status = com.timefold.common.enums.CapsuleStatus.CANCELLED, "
            + "c.cancelledAtUtc = :now, c.updatedAtUtc = :now "
            + "WHERE c.id = :capsuleId "
            + "AND c.owner.id = :ownerId "
            + "AND c.status = com.timefold.common.enums.CapsuleStatus.SEALED")
    int cancelSealedCapsuleAtomically(
            @Param("capsuleId") Long capsuleId,
            @Param("ownerId") Long ownerId,
            @Param("now") Instant now
    );

}