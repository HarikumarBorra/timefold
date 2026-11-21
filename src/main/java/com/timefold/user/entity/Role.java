package com.timefold.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "roles")
public class Role {

	public static final String ROLE_USER = "ROLE_USER";
	public static final String ROLE_ADMIN = "ROLE_ADMIN";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Short id;

	@Column(name = "name", nullable = false, unique = true, length = 50)
	private String name;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "created_at_utc", nullable = false, updatable = false)
	private Instant createdAtUtc;

	@PrePersist
	protected void onCreate() {
		if (this.createdAtUtc == null) {
			this.createdAtUtc = Instant.now();
		}
	}

	public Short getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Instant getCreatedAtUtc() {
		return createdAtUtc;
	}

}