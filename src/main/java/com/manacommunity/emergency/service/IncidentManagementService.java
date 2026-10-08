package com.manacommunity.emergency.service;

import com.manacommunity.emergency.dto.*;
import com.manacommunity.emergency.enums.EmergencyPriority;
import com.manacommunity.emergency.enums.EmergencyStatus;
import com.manacommunity.emergency.enums.EmergencyType;
import com.manacommunity.emergency.enums.EscalationLevel;
import com.manacommunity.emergency.model.*;
import com.manacommunity.emergency.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentManagementService {

    private final EmergencyIncidentRepository incidentRepository;
    private final EmergencyIncidentStatusHistoryRepository timelineRepository;
    private final EmergencyAssignmentRepository assignmentRepository;
    private final EmergencyEvidenceRepository evidenceRepository;
    private final EmergencyClosureReportRepository closureReportRepository;
    private final EmergencyResponderRepository responderRepository;
    private final EmergencyEscalationConfigRepository configRepository;
    private final EmergencyRoutingEngine routingEngine;
    private final SimpMessagingTemplate messagingTemplate;

    private static final AtomicLong sequence = new AtomicLong(0);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Trigger an Emergency Incident (1-tap or 2s-hold SOS).
     */
    @Transactional
    public IncidentResponse createIncident(Long communityId, Long userId, String userName, String userPhone,
                                           CreateIncidentRequest request) {
        String incidentRef = generateIncidentRef();

        EmergencyPriority priority = request.getPriority() != null ? request.getPriority()
                : defaultPriorityForType(request.getEmergencyType());

        // Calculate initial SLA deadline
        int ackTimeoutSeconds = configRepository.findByCommunityIdAndEmergencyType(communityId, request.getEmergencyType())
                .map(EmergencyEscalationConfig::getAckTimeoutSeconds)
                .orElse(120);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusSeconds(ackTimeoutSeconds);

        EmergencyIncident incident = EmergencyIncident.builder()
                .incidentRef(incidentRef)
                .communityId(communityId)
                .reportedByUserId(userId)
                .reporterName(Boolean.TRUE.equals(request.getIsAnonymous()) ? "Anonymous Resident" : userName)
                .reporterPhone(Boolean.TRUE.equals(request.getIsAnonymous()) ? null : userPhone)
                .emergencyType(request.getEmergencyType())
                .priority(priority)
                .status(EmergencyStatus.TRIGGERED)
                .title(request.getTitle() != null ? request.getTitle() : request.getEmergencyType().name().replace("_", " "))
                .description(request.getDescription())
                .isAnonymous(request.getIsAnonymous())
                .tower(request.getTower())
                .floor(request.getFloor())
                .flatNumber(request.getFlatNumber())
                .commonAreaName(request.getCommonAreaName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationNotes(request.getLocationNotes())
                .triggerSource(request.getTriggerSource() != null ? request.getTriggerSource() : "APP_ONE_TAP")
                .slaBreached(false)
                .currentEscalationLevel(EscalationLevel.LEVEL_1_SECURITY)
                .escalationDeadline(deadline)
                .build();

        incident = incidentRepository.save(incident);

        // Record Initial Timeline entry
        logTimeline(incident.getId(), communityId, null, EmergencyStatus.TRIGGERED,
                userId, userName, "RESIDENT", "Emergency raised by resident");

        log.info("Emergency incident triggered: {} ({}) for community {}",
                incidentRef, request.getEmergencyType(), communityId);

        // Broadcast to Command Center over WebSocket
        broadcastToCommandCenter(communityId, toResponse(incident));

        return toResponse(incident);
    }

    /**
     * Update incident status (e.g. ACKNOWLEDGED, ON_SCENE, RESOLVED, CLOSED).
     */
    @Transactional
    public IncidentResponse updateStatus(Long communityId, Long incidentId, Long operatorUserId,
                                         String operatorName, String operatorRole,
                                         IncidentStatusUpdateRequest request) {
        EmergencyIncident incident = getIncidentOrThrow(incidentId, communityId);
        EmergencyStatus oldStatus = incident.getStatus();
        EmergencyStatus newStatus = request.getNewStatus();

        if (oldStatus == newStatus) {
            return toResponse(incident);
        }

        LocalDateTime now = LocalDateTime.now();
        incident.setStatus(newStatus);

        switch (newStatus) {
            case ACKNOWLEDGED -> {
                incident.setAcknowledgedAt(now);
                incident.setAcknowledgedByUserId(operatorUserId);
                // Set next SLA deadline for assignment
                int assignTimeout = configRepository.findByCommunityIdAndEmergencyType(communityId, incident.getEmergencyType())
                        .map(EmergencyEscalationConfig::getAssignTimeoutSeconds).orElse(300);
                incident.setEscalationDeadline(now.plusSeconds(assignTimeout));
            }
            case ON_SCENE -> {
                incident.setOnSceneAt(now);
                // Clear escalation deadline once on scene
                incident.setEscalationDeadline(null);
            }
            case RESOLVED -> {
                incident.setResolvedAt(now);
                incident.setEscalationDeadline(null);
            }
            case CLOSED -> {
                incident.setClosedAt(now);
                incident.setEscalationDeadline(null);
            }
            default -> {}
        }

        incident = incidentRepository.save(incident);

        logTimeline(incidentId, communityId, oldStatus, newStatus,
                operatorUserId, operatorName, operatorRole, request.getComment());

        broadcastToCommandCenter(communityId, toResponse(incident));
        return toResponse(incident);
    }

    /**
     * Assign a responder to an active incident.
     */
    @Transactional
    public IncidentResponse assignResponder(Long communityId, Long incidentId, Long assignedByUserId,
                                            String assignerName, AssignResponderRequest request) {
        EmergencyIncident incident = getIncidentOrThrow(incidentId, communityId);

        EmergencyResponder responder = responderRepository.findById(request.getResponderId())
                .orElseThrow(() -> new RuntimeException("Responder not found: " + request.getResponderId()));

        if (!responder.getCommunityId().equals(communityId)) {
            throw new RuntimeException("Responder does not belong to this community");
        }

        EmergencyAssignment assignment = EmergencyAssignment.builder()
                .incidentId(incidentId)
                .communityId(communityId)
                .responderId(responder.getId())
                .assignedByUserId(assignedByUserId)
                .isLead(Boolean.TRUE.equals(request.getIsLead()))
                .notes(request.getNotes())
                .build();

        assignmentRepository.save(assignment);

        // Update responder load
        responder.setCurrentActiveIncidents(responder.getCurrentActiveIncidents() + 1);
        responderRepository.save(responder);

        // Transition status if currently TRIGGERED or ACKNOWLEDGED
        if (incident.getStatus() == EmergencyStatus.TRIGGERED || incident.getStatus() == EmergencyStatus.ACKNOWLEDGED) {
            EmergencyStatus old = incident.getStatus();
            incident.setStatus(EmergencyStatus.RESPONDER_ASSIGNED);
            incident.setAssignedAt(LocalDateTime.now());

            int onSceneTimeout = configRepository.findByCommunityIdAndEmergencyType(communityId, incident.getEmergencyType())
                    .map(EmergencyEscalationConfig::getOnSceneTimeoutSeconds).orElse(600);
            incident.setEscalationDeadline(LocalDateTime.now().plusSeconds(onSceneTimeout));
            incidentRepository.save(incident);

            logTimeline(incidentId, communityId, old, EmergencyStatus.RESPONDER_ASSIGNED,
                    assignedByUserId, assignerName, "SECURITY_DISPATCHER",
                    "Assigned responder: " + responder.getName() + " (" + responder.getRole() + ")");
        }

        broadcastToCommandCenter(communityId, toResponse(incident));
        return toResponse(incident);
    }

    /**
     * Automated intelligent dispatch of eligible on-duty responder.
     */
    @Transactional
    public IncidentResponse autoDispatch(Long communityId, Long incidentId, Long dispatchedByUserId, String dispatcherName) {
        EmergencyIncident incident = getIncidentOrThrow(incidentId, communityId);
        List<EmergencyResponder> eligible = routingEngine.findEligibleResponders(communityId, incident.getEmergencyType());
        if (eligible.isEmpty()) {
            throw new RuntimeException("No eligible on-duty responders found for emergency type: " + incident.getEmergencyType());
        }

        // Pick responder with the least active incident load
        EmergencyResponder chosen = eligible.stream()
                .min(Comparator.comparingInt(EmergencyResponder::getCurrentActiveIncidents))
                .orElse(eligible.get(0));

        AssignResponderRequest req = new AssignResponderRequest();
        req.setResponderId(chosen.getId());
        req.setIsLead(true);
        req.setNotes("Automated intelligent dispatch for " + incident.getEmergencyType());

        return assignResponder(communityId, incidentId, dispatchedByUserId, dispatcherName, req);
    }

    /**
     * Explicit acknowledgment of emergency by Security Dispatcher / Guard.
     */
    @Transactional
    public IncidentResponse acknowledgeIncident(Long communityId, Long incidentId, Long acknowledgedByUserId,
                                                String acknowledgedByName, String role) {
        IncidentStatusUpdateRequest req = new IncidentStatusUpdateRequest();
        req.setNewStatus(EmergencyStatus.ACKNOWLEDGED);
        req.setComment("Emergency acknowledged by " + role + ": " + acknowledgedByName);
        return updateStatus(communityId, incidentId, acknowledgedByUserId, acknowledgedByName, role, req);
    }

    /**
     * Responder accepts dispatch and commits to an ETA (minutes).
     * Transitions incident to RESPONDER_EN_ROUTE.
     */
    @Transactional
    public IncidentResponse responderAcceptAndSetEta(Long communityId, Long incidentId, Long responderUserId,
                                                     String responderName, int etaMinutes) {
        EmergencyIncident incident = getIncidentOrThrow(incidentId, communityId);
        EmergencyStatus oldStatus = incident.getStatus();
        LocalDateTime now = LocalDateTime.now();

        incident.setStatus(EmergencyStatus.RESPONDER_EN_ROUTE);
        incident.setEnRouteAt(now);
        incident.setEtaMinutes(etaMinutes);
        incident.setEstimatedArrivalAt(now.plusMinutes(etaMinutes));
        incident = incidentRepository.save(incident);

        // Update corresponding assignment if exists
        List<EmergencyAssignment> assignments = assignmentRepository.findByIncidentId(incidentId);
        for (EmergencyAssignment a : assignments) {
            a.setAcknowledgedAt(now);
            a.setEnRouteAt(now);
            a.setEtaMinutes(etaMinutes);
            a.setEstimatedArrivalAt(now.plusMinutes(etaMinutes));
            assignmentRepository.save(a);
        }

        logTimeline(incidentId, communityId, oldStatus, EmergencyStatus.RESPONDER_EN_ROUTE,
                responderUserId, responderName, "RESPONDER",
                String.format("Responder en route. ETA: %d minutes (Expected: %s)", etaMinutes, incident.getEstimatedArrivalAt().toLocalTime()));

        broadcastToCommandCenter(communityId, toResponse(incident));
        return toResponse(incident);
    }

    /**
     * Responder marks arrival on scene.
     * Transitions incident to ON_SCENE and clears arrival SLA countdown.
     */
    @Transactional
    public IncidentResponse markOnScene(Long communityId, Long incidentId, Long responderUserId, String responderName) {
        EmergencyIncident incident = getIncidentOrThrow(incidentId, communityId);
        EmergencyStatus oldStatus = incident.getStatus();
        LocalDateTime now = LocalDateTime.now();

        incident.setStatus(EmergencyStatus.ON_SCENE);
        incident.setOnSceneAt(now);
        incident.setEscalationDeadline(null); // Clear watchdog deadline once on scene
        incident = incidentRepository.save(incident);

        List<EmergencyAssignment> assignments = assignmentRepository.findByIncidentId(incidentId);
        for (EmergencyAssignment a : assignments) {
            if (a.getOnSceneAt() == null) {
                a.setOnSceneAt(now);
                assignmentRepository.save(a);
            }
        }

        logTimeline(incidentId, communityId, oldStatus, EmergencyStatus.ON_SCENE,
                responderUserId, responderName, "RESPONDER", "Responder arrived on scene");

        broadcastToCommandCenter(communityId, toResponse(incident));
        return toResponse(incident);
    }

    /**
     * Responder marks intervention under active progress.
     */
    @Transactional
    public IncidentResponse markUnderAction(Long communityId, Long incidentId, Long responderUserId,
                                            String responderName, String actionNotes) {
        EmergencyIncident incident = getIncidentOrThrow(incidentId, communityId);
        EmergencyStatus oldStatus = incident.getStatus();

        incident.setStatus(EmergencyStatus.UNDER_ACTION);
        incident = incidentRepository.save(incident);

        logTimeline(incidentId, communityId, oldStatus, EmergencyStatus.UNDER_ACTION,
                responderUserId, responderName, "RESPONDER",
                actionNotes != null ? actionNotes : "Emergency intervention in progress");

        broadcastToCommandCenter(communityId, toResponse(incident));
        return toResponse(incident);
    }

    /**
     * Mark emergency resolved on ground.
     * Releases responder workload.
     */
    @Transactional
    public IncidentResponse resolveIncident(Long communityId, Long incidentId, Long responderUserId,
                                            String responderName, String resolutionNotes) {
        EmergencyIncident incident = getIncidentOrThrow(incidentId, communityId);
        EmergencyStatus oldStatus = incident.getStatus();
        LocalDateTime now = LocalDateTime.now();

        incident.setStatus(EmergencyStatus.RESOLVED);
        incident.setResolvedAt(now);
        incident.setEscalationDeadline(null);
        incident = incidentRepository.save(incident);

        // Free up assigned responders' active counts
        List<EmergencyAssignment> assignments = assignmentRepository.findByIncidentId(incidentId);
        for (EmergencyAssignment a : assignments) {
            responderRepository.findById(a.getResponderId()).ifPresent(r -> {
                r.setCurrentActiveIncidents(Math.max(0, r.getCurrentActiveIncidents() - 1));
                responderRepository.save(r);
            });
        }

        logTimeline(incidentId, communityId, oldStatus, EmergencyStatus.RESOLVED,
                responderUserId, responderName, "RESPONDER",
                resolutionNotes != null ? resolutionNotes : "Emergency successfully resolved");

        broadcastToCommandCenter(communityId, toResponse(incident));
        return toResponse(incident);
    }

    /**
     * Manual escalation trigger by security dispatcher / operator.
     */
    @Transactional
    public IncidentResponse escalateIncidentManually(Long communityId, Long incidentId, Long userId,
                                                     String userName, String role,
                                                     EscalationLevel targetLevel, String reason) {
        EmergencyIncident incident = getIncidentOrThrow(incidentId, communityId);
        EmergencyStatus oldStatus = incident.getStatus();

        incident.setCurrentEscalationLevel(targetLevel != null ? targetLevel : EscalationLevel.LEVEL_2_SUPERVISOR);
        incident.setStatus(EmergencyStatus.ESCALATED);
        incident.setSlaBreached(true);
        incident.setEscalationDeadline(LocalDateTime.now().plusMinutes(5));
        incident = incidentRepository.save(incident);

        logTimeline(incidentId, communityId, oldStatus, EmergencyStatus.ESCALATED,
                userId, userName, role, "Manual escalation to " + incident.getCurrentEscalationLevel() + ": " + reason);

        broadcastToCommandCenter(communityId, toResponse(incident));
        return toResponse(incident);
    }

    /**
     * Generate full immutable audit and KPI post-mortem report.
     */
    public IncidentAuditReportResponse generateAuditReport(Long communityId, Long incidentId) {
        EmergencyIncident incident = getIncidentOrThrow(incidentId, communityId);
        List<EmergencyIncidentStatusHistory> timeline = timelineRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId);
        List<EmergencyAssignment> assignments = assignmentRepository.findByIncidentId(incidentId);
        EmergencyClosureReport closure = closureReportRepository.findByIncidentId(incidentId).orElse(null);

        LocalDateTime created = incident.getCreatedAt();
        Long timeToAck = (incident.getAcknowledgedAt() != null && created != null)
                ? Duration.between(created, incident.getAcknowledgedAt()).getSeconds() : null;
        Long timeToDispatch = (incident.getAssignedAt() != null && created != null)
                ? Duration.between(created, incident.getAssignedAt()).getSeconds() : null;
        Long timeToOnScene = (incident.getOnSceneAt() != null && created != null)
                ? Duration.between(created, incident.getOnSceneAt()).getSeconds() : null;
        Long totalResTime = (incident.getResolvedAt() != null && created != null)
                ? Duration.between(created, incident.getResolvedAt()).getSeconds() : null;

        boolean slaMet = !Boolean.TRUE.equals(incident.getSlaBreached());

        String summary = String.format("Incident %s (%s, %s): Ack in %ss, On-Scene in %ss, Resolved in %ss. SLA Met: %s",
                incident.getIncidentRef(), incident.getEmergencyType(), incident.getPriority(),
                timeToAck != null ? timeToAck : "N/A",
                timeToOnScene != null ? timeToOnScene : "N/A",
                totalResTime != null ? totalResTime : "N/A",
                slaMet ? "YES" : "NO");

        return IncidentAuditReportResponse.builder()
                .incident(toResponse(incident))
                .timeline(timeline)
                .assignments(assignments)
                .closureReport(closure)
                .timeToAcknowledgeSeconds(timeToAck)
                .timeToDispatchSeconds(timeToDispatch)
                .timeToOnSceneSeconds(timeToOnScene)
                .totalResolutionTimeSeconds(totalResTime)
                .slaAdherence(slaMet)
                .auditSummary(summary)
                .build();
    }

    /**
     * Cancel an accidental emergency / Report false alarm.
     * Retains immutable audit history.
     */
    @Transactional
    public IncidentResponse cancelIncident(Long communityId, Long incidentId, Long userId,
                                           String userName, String reason, boolean isFalseAlarm) {
        EmergencyIncident incident = getIncidentOrThrow(incidentId, communityId);

        EmergencyStatus newStatus = isFalseAlarm ? EmergencyStatus.FALSE_ALARM : EmergencyStatus.CANCELLED;
        EmergencyStatus oldStatus = incident.getStatus();

        incident.setStatus(newStatus);
        incident.setCancelledAt(LocalDateTime.now());
        incident.setCancellationReason(reason);
        incident.setEscalationDeadline(null);

        incident = incidentRepository.save(incident);

        logTimeline(incidentId, communityId, oldStatus, newStatus,
                userId, userName, "RESIDENT",
                (isFalseAlarm ? "Reported False Alarm: " : "Cancelled Emergency: ") + reason);

        log.info("Incident {} marked as {} by user {}", incident.getIncidentRef(), newStatus, userId);
        broadcastToCommandCenter(communityId, toResponse(incident));
        return toResponse(incident);
    }

    public IncidentDetailResponse getIncidentDetail(Long communityId, Long incidentId) {
        EmergencyIncident incident = getIncidentOrThrow(incidentId, communityId);
        List<EmergencyIncidentStatusHistory> timeline = timelineRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId);
        List<EmergencyAssignment> assignments = assignmentRepository.findByIncidentId(incidentId);
        List<EmergencyEvidence> evidence = evidenceRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId);
        EmergencyClosureReport closure = closureReportRepository.findByIncidentId(incidentId).orElse(null);

        return IncidentDetailResponse.builder()
                .incident(toResponse(incident))
                .timeline(timeline)
                .assignments(assignments)
                .evidence(evidence)
                .closureReport(closure)
                .build();
    }

    public Page<IncidentResponse> getMyIncidents(Long communityId, Long userId, Pageable pageable) {
        return incidentRepository.findByCommunityIdAndReportedByUserIdOrderByCreatedAtDesc(communityId, userId, pageable)
                .map(this::toResponse);
    }

    public Page<IncidentResponse> getCommunityIncidents(Long communityId, Pageable pageable) {
        return incidentRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, pageable)
                .map(this::toResponse);
    }

    public void logTimeline(Long incidentId, Long communityId, EmergencyStatus from, EmergencyStatus to,
                            Long userId, String userName, String role, String comment) {
        EmergencyIncidentStatusHistory history = EmergencyIncidentStatusHistory.builder()
                .incidentId(incidentId)
                .communityId(communityId)
                .fromStatus(from)
                .toStatus(to)
                .changedByUserId(userId)
                .changedByName(userName)
                .changedByRole(role)
                .comment(comment)
                .build();
        timelineRepository.save(history);
    }

    private EmergencyIncident getIncidentOrThrow(Long incidentId, Long communityId) {
        EmergencyIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Emergency incident not found: " + incidentId));
        if (!incident.getCommunityId().equals(communityId)) {
            throw new RuntimeException("Incident does not belong to this community");
        }
        return incident;
    }

    private String generateIncidentRef() {
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        long seq = sequence.incrementAndGet();
        return String.format("INC-%s-%06d", dateStr, seq);
    }

    private EmergencyPriority defaultPriorityForType(EmergencyType type) {
        return switch (type) {
            case SOS_GENERAL, FIRE, MEDICAL, WOMEN_SAFETY -> EmergencyPriority.CRITICAL;
            case CHILD_SAFETY, SECURITY, LIFT_EMERGENCY -> EmergencyPriority.HIGH;
            case WATER_LEAKAGE, POWER_OUTAGE -> EmergencyPriority.MEDIUM;
            default -> EmergencyPriority.LOW;
        };
    }

    public IncidentResponse toResponse(EmergencyIncident i) {
        return IncidentResponse.builder()
                .id(i.getId())
                .incidentRef(i.getIncidentRef())
                .communityId(i.getCommunityId())
                .reportedByUserId(i.getReportedByUserId())
                .reporterName(i.getReporterName())
                .reporterPhone(i.getReporterPhone())
                .emergencyType(i.getEmergencyType())
                .priority(i.getPriority())
                .status(i.getStatus())
                .title(i.getTitle())
                .description(i.getDescription())
                .isAnonymous(i.getIsAnonymous())
                .tower(i.getTower())
                .floor(i.getFloor())
                .flatNumber(i.getFlatNumber())
                .commonAreaName(i.getCommonAreaName())
                .latitude(i.getLatitude())
                .longitude(i.getLongitude())
                .locationNotes(i.getLocationNotes())
                .triggerSource(i.getTriggerSource())
                .slaBreached(i.getSlaBreached())
                .currentEscalationLevel(i.getCurrentEscalationLevel())
                .escalationDeadline(i.getEscalationDeadline())
                .acknowledgedAt(i.getAcknowledgedAt())
                .assignedAt(i.getAssignedAt())
                .etaMinutes(i.getEtaMinutes())
                .estimatedArrivalAt(i.getEstimatedArrivalAt())
                .enRouteAt(i.getEnRouteAt())
                .onSceneAt(i.getOnSceneAt())
                .resolvedAt(i.getResolvedAt())
                .closedAt(i.getClosedAt())
                .cancelledAt(i.getCancelledAt())
                .cancellationReason(i.getCancellationReason())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }

    private void broadcastToCommandCenter(Long communityId, IncidentResponse response) {
        try {
            messagingTemplate.convertAndSend("/topic/community/" + communityId + "/emergency", response);
        } catch (Exception e) {
            log.warn("WebSocket broadcast failed: {}", e.getMessage());
        }
    }
}
