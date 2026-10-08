package com.manacommunity.emergency.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_assignments", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "responder_id", nullable = false)
    private Long responderId;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "eta_minutes")
    private Integer etaMinutes;

    @Column(name = "estimated_arrival_at")
    private LocalDateTime estimatedArrivalAt;

    @Column(name = "en_route_at")
    private LocalDateTime enRouteAt;

    @Column(name = "on_scene_at")
    private LocalDateTime onSceneAt;

    @Column(name = "is_lead")
    @Builder.Default
    private Boolean isLead = false;

    @Column(name = "notes", length = 500)
    private String notes;
}
