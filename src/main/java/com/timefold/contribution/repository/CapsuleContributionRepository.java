package com.timefold.contribution.repository;

import com.timefold.common.enums.ContributionStatus;
import com.timefold.contribution.entity.CapsuleContribution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CapsuleContributionRepository extends JpaRepository<CapsuleContribution, Long> {

    Optional<CapsuleContribution> findFirstByCapsuleIdAndContributorIdOrderByIdAsc(
            Long capsuleId, Long contributorId);

    @Query("SELECT cc FROM CapsuleContribution cc JOIN FETCH cc.contributor "
            + "WHERE cc.publicId = :publicId AND cc.capsule.id = :capsuleId")
    Optional<CapsuleContribution> findByPublicIdAndCapsuleIdWithContributor(
            @Param("publicId") String publicId,
            @Param("capsuleId") Long capsuleId
    );

    @Query("SELECT cc FROM CapsuleContribution cc JOIN FETCH cc.contributor "
            + "WHERE cc.capsule.id = :capsuleId AND cc.status = :status ORDER BY cc.createdAtUtc ASC")
    List<CapsuleContribution> findByCapsuleIdAndStatusWithContributor(
            @Param("capsuleId") Long capsuleId,
            @Param("status") ContributionStatus status
    );

    long countByCapsuleIdAndStatus(Long capsuleId, ContributionStatus status);

}