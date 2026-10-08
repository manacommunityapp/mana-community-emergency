package com.manacommunity.emergency.unit;

import com.manacommunity.emergency.dto.*;
import com.manacommunity.emergency.enums.*;
import com.manacommunity.emergency.model.*;
import com.manacommunity.emergency.repository.*;
import com.manacommunity.emergency.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyWorkflowTest {

    @Mock
    private EmergencyIncidentRepository incidentRepository;
    @Mock
    private EmergencyIncidentStatusHistoryRepository timelineRepository;
    @Mock
    private EmergencyAssignmentRepository assignmentRepository;
    @Mock
    private EmergencyEvidenceRepository evidenceRepository;
    @Mock
    private EmergencyClosureReportRepository closureReportRepository;
    @Mock
    private EmergencyResponderRepository responderRepository;
    @Mock
    private EmergencyEscalationConfigRepository configRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private EmergencyRoutingEngine routingEngine;
    private IncidentManagementService incidentService;
    private ClosureReportService closureService;

    private final Long communityId = 101L;
    private final Long residentUserId = 501L;
    private final Long guardUserId = 601L;
    private final Long adminUserId = 701L;

    @BeforeEach
    void setUp() {
        routingEngine = new EmergencyRoutingEngine(configRepository, responderRepository);
        incidentService = new IncidentManagementService(
                incidentRepository,
                timelineRepository,
                assignmentRepository,
                evidenceRepository,
                closureReportRepository,
                responderRepository,
                configRepository,
                routingEngine,
                messagingTemplate
        );
        closureService = new ClosureReportService(
                closureReportRepository,
                incidentRepository,
                incidentService
        );
    }

    @Test
    @DisplayName("Complete 10-Stage Emergency Lifecycle: SOS -> Location -> Incident -> Severity -> Responder -> Ack -> ETA -> Escalation -> Resolution -> Audit")
    void testEndToEndSafetyCriticalEmergencyWorkflow() {
        // -------------------------------------------------------------
        // 1 & 2. SOS Trigger & Hierarchical Location Capture
        // -------------------------------------------------------------
        CreateIncidentRequest sosRequest = new CreateIncidentRequest();
        sosRequest.setEmergencyType(EmergencyType.MEDICAL);
        sosRequest.setTitle("Chest Pain in Flat A-502");
        sosRequest.setDescription("Senior citizen experiencing acute chest tightness");
        sosRequest.setTower("Tower A");
        sosRequest.setFloor("5");
        sosRequest.setFlatNumber("A-502");
        sosRequest.setLatitude(12.9716);
        sosRequest.setLongitude(77.5946);
        sosRequest.setLocationNotes("Near East Wing Lift Lobby");
        sosRequest.setTriggerSource("APP_HOLD_2S");

        when(configRepository.findByCommunityIdAndEmergencyType(eq(communityId), eq(EmergencyType.MEDICAL)))
                .thenReturn(Optional.empty()); // defaults apply

        when(incidentRepository.save(any(EmergencyIncident.class))).thenAnswer(invocation -> {
            EmergencyIncident i = invocation.getArgument(0);
            if (i.getId() == null) i.setId(1001L);
            if (i.getCreatedAt() == null) i.setCreatedAt(LocalDateTime.now().minusMinutes(10));
            return i;
        });

        IncidentResponse sosResponse = incidentService.createIncident(
                communityId, residentUserId, "Sandesh Patil", "+91 98450 11223", sosRequest);

        assertNotNull(sosResponse);
        assertEquals(1001L, sosResponse.getId());
        assertTrue(sosResponse.getIncidentRef().startsWith("INC-"));
        assertEquals(EmergencyStatus.TRIGGERED, sosResponse.getStatus());
        assertEquals(EmergencyPriority.CRITICAL, sosResponse.getPriority());
        assertEquals("Tower A", sosResponse.getTower());
        assertEquals("A-502", sosResponse.getFlatNumber());
        assertEquals("APP_HOLD_2S", sosResponse.getTriggerSource());
        assertFalse(sosResponse.getSlaBreached());
        assertNotNull(sosResponse.getEscalationDeadline());

        // Verify timeline recorded SOS trigger
        verify(timelineRepository, atLeastOnce()).save(any(EmergencyIncidentStatusHistory.class));

        // -------------------------------------------------------------
        // 3. Severity & Automated Routing Setup
        // -------------------------------------------------------------
        EmergencyIncident incident = EmergencyIncident.builder()
                .id(1001L)
                .incidentRef(sosResponse.getIncidentRef())
                .communityId(communityId)
                .reportedByUserId(residentUserId)
                .reporterName("Sandesh Patil")
                .reporterPhone("+91 98450 11223")
                .emergencyType(EmergencyType.MEDICAL)
                .priority(EmergencyPriority.CRITICAL)
                .status(EmergencyStatus.TRIGGERED)
                .tower("Tower A")
                .floor("5")
                .flatNumber("A-502")
                .triggerSource("APP_HOLD_2S")
                .slaBreached(false)
                .currentEscalationLevel(EscalationLevel.LEVEL_1_SECURITY)
                .escalationDeadline(LocalDateTime.now().plusSeconds(120))
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(incidentRepository.findById(1001L)).thenReturn(Optional.of(incident));

        // -------------------------------------------------------------
        // 4. Responder Dispatch (Automated Intelligent Routing)
        // -------------------------------------------------------------
        EmergencyResponder paramedic = EmergencyResponder.builder()
                .id(201L)
                .communityId(communityId)
                .userId(guardUserId)
                .name("Nurse Anita")
                .phone("+91 98800 55443")
                .role(ResponderRole.MEDICAL_FIRST_RESPONDER)
                .badgeNumber("MED-09")
                .isAvailable(true)
                .isOnDuty(true)
                .currentActiveIncidents(0)
                .build();

        when(responderRepository.findByCommunityIdAndRoleAndIsAvailableTrueAndIsOnDutyTrue(
                communityId, ResponderRole.MEDICAL_FIRST_RESPONDER))
                .thenReturn(List.of(paramedic));
        when(responderRepository.findById(201L)).thenReturn(Optional.of(paramedic));

        IncidentResponse dispatchResponse = incidentService.autoDispatch(
                communityId, 1001L, guardUserId, "Control Room Operator");

        assertEquals(EmergencyStatus.RESPONDER_ASSIGNED, dispatchResponse.getStatus());
        assertEquals(1, paramedic.getCurrentActiveIncidents());
        verify(assignmentRepository).save(any(EmergencyAssignment.class));

        // -------------------------------------------------------------
        // 5. Explicit Acknowledgement
        // -------------------------------------------------------------
        IncidentResponse ackResponse = incidentService.acknowledgeIncident(
                communityId, 1001L, guardUserId, "Nurse Anita", "MEDICAL_FIRST_RESPONDER");

        assertEquals(EmergencyStatus.ACKNOWLEDGED, ackResponse.getStatus());
        assertNotNull(incident.getAcknowledgedAt());
        assertEquals(guardUserId, incident.getAcknowledgedByUserId());

        // -------------------------------------------------------------
        // 6. Responder Acceptance & ETA Committal (En Route)
        // -------------------------------------------------------------
        when(assignmentRepository.findByIncidentId(1001L)).thenReturn(List.of(
                EmergencyAssignment.builder()
                        .id(301L)
                        .incidentId(1001L)
                        .communityId(communityId)
                        .responderId(201L)
                        .isLead(true)
                        .build()
        ));

        IncidentResponse etaResponse = incidentService.responderAcceptAndSetEta(
                communityId, 1001L, guardUserId, "Nurse Anita", 4);

        assertEquals(EmergencyStatus.RESPONDER_EN_ROUTE, etaResponse.getStatus());
        assertEquals(4, etaResponse.getEtaMinutes());
        assertNotNull(etaResponse.getEstimatedArrivalAt());
        assertNotNull(etaResponse.getEnRouteAt());

        // -------------------------------------------------------------
        // 7. Watchdog / Manual Escalation
        // -------------------------------------------------------------
        IncidentResponse escResponse = incidentService.escalateIncidentManually(
                communityId, 1001L, guardUserId, "Nurse Anita", "RESPONDER",
                EscalationLevel.LEVEL_2_SUPERVISOR, "Suspected cardiac arrest; ambulance requested");

        assertEquals(EmergencyStatus.ESCALATED, escResponse.getStatus());
        assertEquals(EscalationLevel.LEVEL_2_SUPERVISOR, escResponse.getCurrentEscalationLevel());
        assertTrue(escResponse.getSlaBreached());

        // -------------------------------------------------------------
        // 8. On-Scene Arrival & Remediation Under Action
        // -------------------------------------------------------------
        IncidentResponse sceneResponse = incidentService.markOnScene(
                communityId, 1001L, guardUserId, "Nurse Anita");

        assertEquals(EmergencyStatus.ON_SCENE, sceneResponse.getStatus());
        assertNotNull(sceneResponse.getOnSceneAt());
        assertNull(incident.getEscalationDeadline()); // Deadline cleared upon physical arrival

        IncidentResponse actionResponse = incidentService.markUnderAction(
                communityId, 1001L, guardUserId, "Nurse Anita", "Administered sublingual nitroglycerin and oxygen");

        assertEquals(EmergencyStatus.UNDER_ACTION, actionResponse.getStatus());

        // -------------------------------------------------------------
        // 9. Resolution & Closure Review
        // -------------------------------------------------------------
        IncidentResponse resolvedResponse = incidentService.resolveIncident(
                communityId, 1001L, guardUserId, "Nurse Anita", "Patient vitals stabilized; handed over to Aster Ambulance");

        assertEquals(EmergencyStatus.RESOLVED, resolvedResponse.getStatus());
        assertNotNull(resolvedResponse.getResolvedAt());
        assertEquals(0, paramedic.getCurrentActiveIncidents()); // Responder freed

        // Submit formal closure report
        ClosureReportRequest closureReq = new ClosureReportRequest();
        closureReq.setResolutionSummary("Patient stabilized and transferred to hospital safely");
        closureReq.setRootCause("Acute angina pectoris episode");
        closureReq.setActionTaken("First responder on scene in 4 minutes, emergency oxygen provided");
        closureReq.setPreventativeMeasures("Recommend periodic cardiac health checks for senior residents");

        when(closureReportRepository.save(any(EmergencyClosureReport.class))).thenAnswer(i -> {
            EmergencyClosureReport r = i.getArgument(0);
            r.setId(401L);
            return r;
        });

        ClosureReportResponse closureResp = closureService.submitClosureReport(
                communityId, 1001L, guardUserId, "Nurse Anita", closureReq);

        assertNotNull(closureResp);
        assertEquals("Patient stabilized and transferred to hospital safely", closureResp.getResolutionSummary());

        // Admin reviews and marks CLOSED
        EmergencyClosureReport savedClosure = EmergencyClosureReport.builder()
                .id(401L)
                .incidentId(1001L)
                .communityId(communityId)
                .resolutionSummary(closureReq.getResolutionSummary())
                .isApproved(false)
                .build();
        when(closureReportRepository.findByIncidentId(1001L)).thenReturn(Optional.of(savedClosure));

        ReviewClosureRequest reviewReq = new ReviewClosureRequest();
        reviewReq.setIsApproved(true);
        reviewReq.setAdminNotes("Commendable rapid response by Nurse Anita and Security Team");

        ClosureReportResponse finalReport = closureService.reviewClosureReport(
                communityId, 1001L, adminUserId, reviewReq);

        assertTrue(finalReport.getIsApproved());
        assertEquals(EmergencyStatus.CLOSED, incident.getStatus());
        assertNotNull(incident.getClosedAt());

        // -------------------------------------------------------------
        // 10. Audit Report & Latency KPI Analytics
        // -------------------------------------------------------------
        EmergencyIncidentStatusHistory h1 = EmergencyIncidentStatusHistory.builder()
                .incidentId(1001L).communityId(communityId).fromStatus(null).toStatus(EmergencyStatus.TRIGGERED).build();
        EmergencyIncidentStatusHistory h2 = EmergencyIncidentStatusHistory.builder()
                .incidentId(1001L).communityId(communityId).fromStatus(EmergencyStatus.TRIGGERED).toStatus(EmergencyStatus.ACKNOWLEDGED).build();
        EmergencyIncidentStatusHistory h3 = EmergencyIncidentStatusHistory.builder()
                .incidentId(1001L).communityId(communityId).fromStatus(EmergencyStatus.ACKNOWLEDGED).toStatus(EmergencyStatus.RESOLVED).build();

        when(timelineRepository.findByIncidentIdOrderByCreatedAtAsc(1001L)).thenReturn(List.of(h1, h2, h3));

        IncidentAuditReportResponse auditReport = incidentService.generateAuditReport(communityId, 1001L);

        assertNotNull(auditReport);
        assertEquals(1001L, auditReport.getIncident().getId());
        assertEquals(3, auditReport.getTimeline().size());
        assertNotNull(auditReport.getTimeToAcknowledgeSeconds());
        assertNotNull(auditReport.getTimeToOnSceneSeconds());
        assertNotNull(auditReport.getTotalResolutionTimeSeconds());
        assertNotNull(auditReport.getAuditSummary());
        assertTrue(auditReport.getAuditSummary().contains("INC-"));
    }
}
