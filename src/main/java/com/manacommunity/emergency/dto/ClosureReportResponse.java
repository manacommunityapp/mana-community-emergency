package com.manacommunity.emergency.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClosureReportResponse {
    private Long id;
    private Long incidentId;
    private Long communityId;
    private Long submittedByUserId;
    private String submittedByName;
    private String resolutionSummary;
    private String rootCause;
    private String actionTaken;
    private String preventativeMeasures;
    private LocalDateTime arrivalTime;
    private LocalDateTime resolvedTime;
    private LocalDateTime submittedAt;
    private Long adminReviewedByUserId;
    private LocalDateTime adminReviewedAt;
    private String adminNotes;
    private Boolean isApproved;
}
