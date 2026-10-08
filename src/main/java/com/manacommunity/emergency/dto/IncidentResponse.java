package com.manacommunity.emergency.dto;

import com.manacommunity.emergency.enums.EmergencyPriority;
import com.manacommunity.emergency.enums.EmergencyStatus;
import com.manacommunity.emergency.enums.EmergencyType;
import com.manacommunity.emergency.enums.EscalationLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IncidentResponse {
    private Long id;
    private String incidentRef;
    private Long communityId;
    private Long reportedByUserId;
    private String reporterName;
    private String reporterPhone;
    private EmergencyType emergencyType;
    private EmergencyPriority priority;
    private EmergencyStatus status;
    private String title;
    private String description;
    private Boolean isAnonymous;
    private String tower;
    private String floor;
    private String flatNumber;
    private String commonAreaName;
    private Double latitude;
    private Double longitude;
    private String locationNotes;
    private String triggerSource;
    private Boolean slaBreached;
    private EscalationLevel currentEscalationLevel;
    private LocalDateTime escalationDeadline;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime assignedAt;
    private Integer etaMinutes;
    private LocalDateTime estimatedArrivalAt;
    private LocalDateTime enRouteAt;
    private LocalDateTime onSceneAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
