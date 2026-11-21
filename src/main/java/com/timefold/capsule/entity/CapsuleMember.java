package com.timefold.capsule.entity;

import com.timefold.common.enums.ContributionVisibility;
import com.timefold.common.enums.InvitationStatus;
import com.timefold.common.enums.MemberRole;
import com.timefold.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "capsule_members")
public class CapsuleMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "capsule_id", nullable = false)
	private Capsule capsule;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "member_role", nullable = false, columnDefinition = "ENUM('OWNER', 'CONTRIBUTOR')")
	private MemberRole memberRole;

	@Column(name = "can_edit_before_seal", nullable = false)
	private boolean canEditBeforeSeal = true;

	@Enumerated(EnumType.STRING)
	@Column(name = "contribution_visibility", nullable = false, columnDefinition = "ENUM('ALL', 'OWN_ONLY', 'CUSTOM')")
	private ContributionVisibility contributionVisibility = ContributionVisibility.ALL;

	@Enumerated(EnumType.STRING)
	@Column(name = "invitation_status", nullable = false, columnDefinition = "ENUM('PENDING', 'ACCEPTED', 'DECLINED', 'REMOVED')")
	private InvitationStatus invitationStatus = InvitationStatus.ACCEPTED;

	@Column(name = "joined_at_utc")
	private Instant joinedAtUtc;

	@Column(name = "created_at_utc", nullable = false, updatable = false)
	private Instant createdAtUtc;

	@Column(name = "updated_at_utc", nullable = false)
	private Instant updatedAtUtc;

	@PrePersist
	protected void onCreate() {
		Instant now = Instant.now();
		if (this.createdAtUtc == null) {
			this.createdAtUtc = now;
		}
		this.updatedAtUtc = now;
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAtUtc = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Capsule getCapsule() {
		return capsule;
	}

	public void setCapsule(Capsule capsule) {
		this.capsule = capsule;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public MemberRole getMemberRole() {
		return memberRole;
	}

	public void setMemberRole(MemberRole memberRole) {
		this.memberRole = memberRole;
	}

	public boolean isCanEditBeforeSeal() {
		return canEditBeforeSeal;
	}

	public void setCanEditBeforeSeal(boolean canEditBeforeSeal) {
		this.canEditBeforeSeal = canEditBeforeSeal;
	}

	public ContributionVisibility getContributionVisibility() {
		return contributionVisibility;
	}

	public void setContributionVisibility(ContributionVisibility contributionVisibility) {
		this.contributionVisibility = contributionVisibility;
	}

	public InvitationStatus getInvitationStatus() {
		return invitationStatus;
	}

	public void setInvitationStatus(InvitationStatus invitationStatus) {
		this.invitationStatus = invitationStatus;
	}

	public Instant getJoinedAtUtc() {
		return joinedAtUtc;
	}

	public void setJoinedAtUtc(Instant joinedAtUtc) {
		this.joinedAtUtc = joinedAtUtc;
	}

	public Instant getCreatedAtUtc() {
		return createdAtUtc;
	}

	public Instant getUpdatedAtUtc() {
		return updatedAtUtc;
	}

}