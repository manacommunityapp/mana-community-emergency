package com.manacommunity.emergency.service;

import com.manacommunity.emergency.enums.EmergencyStatus;
import com.manacommunity.emergency.enums.EscalationLevel;
import com.manacommunity.emergency.model.EmergencyIncident;
import com.manacommunity.emergency.repository.EmergencyIncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background watchdog service that regularly monitors SLA deadlines.
 * Automatically escalates incidents when acknowledgment, assignment, or on-scene arrival is breached.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EscalationWatchdogService {

    private final EmergencyIncidentRepository incidentRepository;
    private final IncidentManagementService incidentService;

    private static final List<EmergencyStatus> ACTIVE_STATUSES = List.of(
            EmergencyStatus.TRIGGERED,
            EmergencyStatus.ACKNOWLEDGED,
            EmergencyStatus.RESPONDER_ASSIGNED,
            EmergencyStatus.RESPONDER_EN_ROUTE
    );

    @Scheduled(fixedRateString = "${emergency.watchdog.check-interval-ms:30000}")
    @Transactional
    public void scanAndEscalateBreachedIncidents() {
        LocalDateTime now = LocalDateTime.now();
        List<EmergencyIncident> breached = incidentRepository.findBreachedIncidents(ACTIVE_STATUSES, now);

        if (breached.isEmpty()) {
            return;
        }

        log.warn("Escalation watchdog found {} SLA-breached emergency incidents", breached.size());

        for (EmergencyIncident incident : breached) {
            escalateIncident(incident);
        }
    }

    private void escalateIncident(EmergencyIncident incident) {
        EscalationLevel current = incident.getCurrentEscalationLevel();
        EscalationLevel next = nextLevel(current);

        if (next == current) {
            // Already at maximum escalation level
            incident.setEscalationDeadline(null);
            incidentRepository.save(incident);
            return;
        }

        EmergencyStatus oldStatus = incident.getStatus();
        incident.setCurrentEscalationLevel(next);
        // Extend deadline for next escalation level by 5 minutes
        incident.setEscalationDeadline(LocalDateTime.now().plusMinutes(5));
        incident.setStatus(EmergencyStatus.ESCALATED);
        incident.setSlaBreached(true);
        incident = incidentRepository.save(incident);

        String logMsg = String.format("SLA breached at %s. Auto-escalated to %s", current, next);
        incidentService.logTimeline(incident.getId(), incident.getCommunityId(),
                oldStatus, EmergencyStatus.ESCALATED,
                null, "SLA_WATCHDOG", "SYSTEM", logMsg);

        log.error("Emergency incident {} ESCALATED to {} (Community: {})",
                incident.getIncidentRef(), next, incident.getCommunityId());

        incidentService.broadcastToCommandCenter(incident.getCommunityId(), incidentService.toResponse(incident));
    }

    private EscalationLevel nextLevel(EscalationLevel current) {
        if (current == null) return EscalationLevel.LEVEL_2_SUPERVISOR;
        return switch (current) {
            case LEVEL_1_SECURITY -> EscalationLevel.LEVEL_2_SUPERVISOR;
            case LEVEL_2_SUPERVISOR -> EscalationLevel.LEVEL_3_ADMIN;
            case LEVEL_3_ADMIN, LEVEL_4_EXTERNAL -> EscalationLevel.LEVEL_4_EXTERNAL;
        };
    }
}
