package com.manacommunity.emergency.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_closure_reports", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyClosureReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false, unique = true)
    private Long incidentId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "submitted_by_user_id", nullable = false)
    private Long submittedByUserId;

    @Column(name = "submitted_by_name", length = 120)
    private String submittedByName;

    @Column(name = "resolution_summary", nullable = false, length = 2000)
    private String resolutionSummary;

    @Column(name = "root_cause", length = 1000)
    private String rootCause;

    @Column(name = "action_taken", nullable = false, length = 2000)
    private String actionTaken;

    @Column(name = "preventative_measures", length = 1000)
    private String preventativeMeasures;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    // Admin Review
    @Column(name = "admin_reviewed_by_user_id")
    private Long adminReviewedByUserId;

    @Column(name = "admin_reviewed_at")
    private LocalDateTime adminReviewedAt;

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @Column(name = "is_approved")
    @Builder.Default
    private Boolean isApproved = true;
}
