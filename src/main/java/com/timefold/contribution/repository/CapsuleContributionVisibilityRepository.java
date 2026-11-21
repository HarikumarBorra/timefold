package com.timefold.contribution.repository;

import com.timefold.contribution.entity.CapsuleContributionVisibility;
import com.timefold.contribution.entity.CapsuleContributionVisibilityId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CapsuleContributionVisibilityRepository
        extends JpaRepository<CapsuleContributionVisibility, CapsuleContributionVisibilityId> {

    @Query("SELECT v FROM CapsuleContributionVisibility v WHERE v.id.contributionId IN :contributionIds")
    List<CapsuleContributionVisibility> findByContributionIds(
            @Param("contributionIds") Collection<Long> contributionIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CapsuleContributionVisibility v WHERE v.id.contributionId = :contributionId")
    void deleteByContributionId(@Param("contributionId") Long contributionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CapsuleContributionVisibility v "
            + "WHERE v.id.allowedUserId = :userId "
            + "AND v.contribution.capsule.id = :capsuleId")
    void deleteByCapsuleIdAndAllowedUserId(@Param("capsuleId") Long capsuleId, @Param("userId") Long userId);
    
}