package com.timefold.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "actor_user_id")
	private Long actorUserId;

	@Column(name = "actor_role", length = 50)
	private String actorRole;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@Column(name = "entity_type", nullable = false, length = 64)
	private String entityType;

	@Column(name = "entity_id", length = 64)
	private String entityId;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "user_agent", length = 255)
	private String userAgent;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata_json", columnDefinition = "JSON")
	private Map<String, Object> metadataJson;

	@Column(name = "created_at_utc", nullable = false, updatable = false)
	private Instant createdAtUtc;

	@PrePersist
	protected void onCreate() {
		if (this.createdAtUtc == null) {
			this.createdAtUtc = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public Long getActorUserId() {
		return actorUserId;
	}

	public void setActorUserId(Long actorUserId) {
		this.actorUserId = actorUserId;
	}

	public String getActorRole() {
		return actorRole;
	}

	public void setActorRole(String actorRole) {
		this.actorRole = actorRole;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public Map<String, Object> getMetadataJson() {
		return metadataJson;
	}

	public void setMetadataJson(Map<String, Object> metadataJson) {
		this.metadataJson = metadataJson;
	}

	public Instant getCreatedAtUtc() {
		return createdAtUtc;
	}

}