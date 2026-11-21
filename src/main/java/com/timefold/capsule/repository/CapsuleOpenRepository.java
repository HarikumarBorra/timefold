package com.timefold.capsule.repository;

import com.timefold.capsule.entity.CapsuleOpen;
import com.timefold.capsule.entity.CapsuleOpenId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CapsuleOpenRepository extends JpaRepository<CapsuleOpen, CapsuleOpenId> {

    Optional<CapsuleOpen> findByCapsuleIdAndUserId(Long capsuleId, Long userId);

    @Query("SELECT co.id.capsuleId FROM CapsuleOpen co "
            + "WHERE co.id.userId = :userId AND co.id.capsuleId IN :capsuleIds")
    List<Long> findOpenedCapsuleIdsForUser(
            @Param("userId") Long userId,
            @Param("capsuleIds") Collection<Long> capsuleIds
    );

    @Query("SELECT co FROM CapsuleOpen co JOIN FETCH co.user "
            + "WHERE co.id.capsuleId = :capsuleId ORDER BY co.firstOpenedAtUtc ASC")
    List<CapsuleOpen> findByCapsuleIdWithUser(@Param("capsuleId") Long capsuleId);

    long countByIdUserId(Long userId);
    
}