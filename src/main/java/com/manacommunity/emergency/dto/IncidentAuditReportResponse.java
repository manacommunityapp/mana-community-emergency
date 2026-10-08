package com.manacommunity.emergency.dto;

import com.manacommunity.emergency.model.EmergencyAssignment;
import com.manacommunity.emergency.model.EmergencyClosureReport;
import com.manacommunity.emergency.model.EmergencyIncidentStatusHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentAuditReportResponse {
    private IncidentResponse incident;
    private List<EmergencyIncidentStatusHistory> timeline;
    private List<EmergencyAssignment> assignments;
    private EmergencyClosureReport closureReport;

    // Performance & SLA Auditing Metrics
    private Long timeToAcknowledgeSeconds;
    private Long timeToDispatchSeconds;
    private Long timeToOnSceneSeconds;
    private Long totalResolutionTimeSeconds;
    private Boolean slaAdherence;
    private String auditSummary;
}
