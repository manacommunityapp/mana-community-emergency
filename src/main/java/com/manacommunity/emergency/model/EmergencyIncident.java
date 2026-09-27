package com.manacommunity.emergency.model;

import com.manacommunity.emergency.enums.EmergencyPriority;
import com.manacommunity.emergency.enums.EmergencyStatus;
import com.manacommunity.emergency.enums.EmergencyType;
import com.manacommunity.emergency.enums.EscalationLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_incidents", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_ref", nullable = false, unique = true, length = 64)
    private String incidentRef; // e.g. INC-20260925-000001

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "reported_by_user_id", nullable = false)
    private Long reportedByUserId;

    @Column(name = "reporter_name", length = 120)
    private String reporterName;

    @Column(name = "reporter_phone", length = 20)
    private String reporterPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "emergency_type", nullable = false, length = 40)
    private EmergencyType emergencyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private EmergencyPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EmergencyStatus status;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "is_anonymous")
    @Builder.Default
    private Boolean isAnonymous = false;

    // Location Hierarchy
    @Column(name = "tower", length = 50)
    private String tower;

    @Column(name = "floor", length = 20)
    private String floor;

    @Column(name = "flat_number", length = 50)
    private String flatNumber;

    @Column(name = "common_area_name", length = 100)
    private String commonAreaName;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "location_notes", length = 500)
    private String locationNotes;

    // Escalation & SLA
    @Enumerated(EnumType.STRING)
    @Column(name = "current_escalation_level", length = 30)
    @Builder.Default
    private EscalationLevel currentEscalationLevel = EscalationLevel.LEVEL_1_SECURITY;

    @Column(name = "escalation_deadline")
    private LocalDateTime escalationDeadline;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "acknowledged_by_user_id")
    private Long acknowledgedByUserId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "on_scene_at")
    private LocalDateTime onSceneAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
