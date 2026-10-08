package com.manacommunity.emergency.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CommandCenterDashboardResponse {
    private Long activeIncidentsCount;
    private Long triggeredCount;
    private Long acknowledgedCount;
    private Long responderAssignedCount;
    private Long enRouteCount;
    private Long onSceneCount;
    private Long escalatedCount;
    private Long resolvedTodayCount;
    private Long totalIncidents30Days;
    private Map<String, Long> incidentsByType;
    private Map<String, Long> availableRespondersByRole;
    private List<IncidentResponse> activeIncidents;
}
