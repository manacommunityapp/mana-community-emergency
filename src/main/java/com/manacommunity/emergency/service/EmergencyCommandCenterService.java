package com.manacommunity.emergency.service;

import com.manacommunity.emergency.dto.CommandCenterDashboardResponse;
import com.manacommunity.emergency.dto.IncidentResponse;
import com.manacommunity.emergency.enums.EmergencyStatus;
import com.manacommunity.emergency.enums.ResponderRole;
import com.manacommunity.emergency.model.EmergencyIncident;
import com.manacommunity.emergency.model.EmergencyResponder;
import com.manacommunity.emergency.repository.EmergencyIncidentRepository;
import com.manacommunity.emergency.repository.EmergencyResponderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmergencyCommandCenterService {

    private final EmergencyIncidentRepository incidentRepository;
    private final EmergencyResponderRepository responderRepository;
    private final IncidentManagementService incidentService;

    private static final List<EmergencyStatus> ACTIVE_STATUSES = List.of(
            EmergencyStatus.TRIGGERED,
            EmergencyStatus.ACKNOWLEDGED,
            EmergencyStatus.RESPONDER_ASSIGNED,
            EmergencyStatus.RESPONDER_EN_ROUTE,
            EmergencyStatus.ON_SCENE,
            EmergencyStatus.UNDER_ACTION,
            EmergencyStatus.ESCALATED
    );

    public CommandCenterDashboardResponse getDashboard(Long communityId) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<EmergencyIncident> activeList = incidentRepository.findByCommunityIdAndStatusIn(communityId, ACTIVE_STATUSES);
        List<IncidentResponse> activeIncidents = activeList.stream()
                .map(incidentService::toResponse)
                .collect(Collectors.toList());

        long triggered = activeList.stream().filter(i -> i.getStatus() == EmergencyStatus.TRIGGERED).count();
        long acknowledged = activeList.stream().filter(i -> i.getStatus() == EmergencyStatus.ACKNOWLEDGED).count();
        long assigned = activeList.stream().filter(i -> i.getStatus() == EmergencyStatus.RESPONDER_ASSIGNED
                || i.getStatus() == EmergencyStatus.RESPONDER_EN_ROUTE).count();
        long onScene = activeList.stream().filter(i -> i.getStatus() == EmergencyStatus.ON_SCENE
                || i.getStatus() == EmergencyStatus.UNDER_ACTION).count();

        long resolvedToday = incidentRepository.countByCommunityAndStatusSince(communityId, EmergencyStatus.RESOLVED, startOfToday)
                + incidentRepository.countByCommunityAndStatusSince(communityId, EmergencyStatus.CLOSED, startOfToday);

        // Grouped by type
        Map<String, Long> incidentsByType = new LinkedHashMap<>();
        for (Object[] row : incidentRepository.countGroupedByType(communityId, thirtyDaysAgo)) {
            incidentsByType.put(row[0].toString(), (Long) row[1]);
        }

        // Available responders by role
        Map<String, Long> availableResponders = new LinkedHashMap<>();
        for (ResponderRole role : ResponderRole.values()) {
            long count = responderRepository
                    .findByCommunityIdAndRoleAndIsAvailableTrueAndIsOnDutyTrue(communityId, role).size();
            availableResponders.put(role.name(), count);
        }

        return CommandCenterDashboardResponse.builder()
                .activeIncidentsCount((long) activeList.size())
                .triggeredCount(triggered)
                .acknowledgedCount(acknowledged)
                .responderAssignedCount(assigned)
                .onSceneCount(onScene)
                .resolvedTodayCount(resolvedToday)
                .totalIncidents30Days(incidentRepository.countByCommunityAndStatusSince(communityId, EmergencyStatus.CLOSED, thirtyDaysAgo))
                .incidentsByType(incidentsByType)
                .availableRespondersByRole(availableResponders)
                .activeIncidents(activeIncidents)
                .build();
    }

    public List<IncidentResponse> getActiveIncidents(Long communityId) {
        return incidentRepository.findByCommunityIdAndStatusIn(communityId, ACTIVE_STATUSES).stream()
                .map(incidentService::toResponse)
                .collect(Collectors.toList());
    }
}
