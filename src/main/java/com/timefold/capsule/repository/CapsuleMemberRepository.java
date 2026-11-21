package com.timefold.capsule.repository;

import com.timefold.capsule.entity.CapsuleMember;
import com.timefold.common.enums.InvitationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CapsuleMemberRepository extends JpaRepository<CapsuleMember, Long> {

	Optional<CapsuleMember> findByCapsuleIdAndUserId(Long capsuleId, Long userId);

	boolean existsByCapsuleIdAndUserIdAndInvitationStatus(Long capsuleId, Long userId, InvitationStatus status);

	@Query("SELECT cm FROM CapsuleMember cm JOIN FETCH cm.user "
			+ "WHERE cm.capsule.id = :capsuleId ORDER BY cm.memberRole ASC, cm.joinedAtUtc ASC")
	List<CapsuleMember> findByCapsuleIdWithUser(@Param("capsuleId") Long capsuleId);

	@Query("SELECT cm FROM CapsuleMember cm JOIN FETCH cm.user "
			+ "WHERE cm.capsule.id = :capsuleId AND cm.invitationStatus = :status "
			+ "ORDER BY cm.memberRole ASC, cm.joinedAtUtc ASC")
	List<CapsuleMember> findByCapsuleIdAndStatusWithUser(@Param("capsuleId") Long capsuleId,
			@Param("status") InvitationStatus status);

	@Query("SELECT COUNT(cm) FROM CapsuleMember cm " + "WHERE cm.user.id = :userId "
			+ "AND cm.invitationStatus = com.timefold.common.enums.InvitationStatus.ACCEPTED "
			+ "AND cm.capsule.capsuleType = com.timefold.common.enums.CapsuleType.SHARED")
	long countAcceptedSharedCapsulesForUser(@Param("userId") Long userId);

}