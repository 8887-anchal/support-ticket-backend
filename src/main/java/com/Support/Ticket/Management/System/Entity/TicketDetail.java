package com.Support.Ticket.Management.System.Entity;

import java.time.Instant;

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

@Entity
@Table(name = "tickets")
public class TicketDetail {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false, length = 160)
	private String title;
	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;
	@Column(nullable = false, length = 80)
	private String category;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TicketPriority priority = TicketPriority.MEDIUM;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TicketStatus status = TicketStatus.OPEN;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_id", nullable = false)
	private AppUser createdBy;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_agent_id")
	private AppUser assignedAgent;
	@Column(nullable = false, updatable = false)
	private Instant createdAt;
	@Column(nullable = false)
	private Instant updatedAt;

	@PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
	@PreUpdate void onUpdate() { updatedAt = Instant.now(); }
	public Long getId() { return id; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getCategory() { return category; }
	public void setCategory(String category) { this.category = category; }
	public TicketPriority getPriority() { return priority; }
	public void setPriority(TicketPriority priority) { this.priority = priority; }
	public TicketStatus getStatus() { return status; }
	public void setStatus(TicketStatus status) { this.status = status; }
	public AppUser getCreatedBy() { return createdBy; }
	public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }
	public AppUser getAssignedAgent() { return assignedAgent; }
	public void setAssignedAgent(AppUser assignedAgent) { this.assignedAgent = assignedAgent; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
