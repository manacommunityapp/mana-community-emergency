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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verification of safety-critical emergency behavior under network failure conditions:
 * - Alert delivery resilience (WebSocket broker disconnect, socket timeout, messaging failure)
 * - Acknowledgement integrity when network broadcast fails
 * - Watchdog SLA breach escalation under communication partitions (Level 1 -> 2 -> 3 -> 4)
 * - End-to-end immutable audit trail provenance and KPI generation despite network failures
 */
@ExtendWith(MockitoExtension.class)
class EmergencyNetworkFailureTest {

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
    private EscalationWatchdogService watchdogService;

    private final Long communityId = 101L;
    private final Long residentUserId = 501L;
    private final Long guardUserId = 601L;
    private final Long supervisorUserId = 701L;

    private final List<EmergencyIncidentStatusHistory> auditTimelineLog = new ArrayList<>();

    @BeforeEach
    void setUp() {
        auditTimelineLog.clear();
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
        watchdogService = new EscalationWatchdogService(incidentRepository, incidentService);

        // Capture all timeline saves into the in-memory audit log
        lenient().when(timelineRepository.save(any(EmergencyIncidentStatusHistory.class))).thenAnswer(invocation -> {
            EmergencyIncidentStatusHistory entry = invocation.getArgument(0);
            if (entry.getId() == null) entry.setId((long) (auditTimelineLog.size() + 1));
            if (entry.getCreatedAt() == null) entry.setCreatedAt(LocalDateTime.now());
            auditTimelineLog.add(entry);
            return entry;
        });

        // Config default
        lenient().when(configRepository.findByCommunityIdAndEmergencyType(anyLong(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Alert Delivery: SOS trigger succeeds and commits when network broadcast throws MessagingException")
    void testAlertDeliveryUnderNetworkFailure_InitialSosTrigger() {
        // Simulate network partition / broker failure
        doThrow(new MessagingException("Broker connection refused / Network timeout"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setEmergencyType(EmergencyType.MEDICAL);
        request.setTitle("Severe Asthma Attack in Tower B");
        request.setDescription("Resident unable to breathe");
        request.setTower("Tower B");
        request.setFlatNumber("B-304");
        request.setTriggerSource("APP_ONE_TAP");

        when(incidentRepository.save(any(EmergencyIncident.class))).thenAnswer(invocation -> {
            EmergencyIncident i = invocation.getArgument(0);
            i.setId(2001L);
            return i;
        });

        // Trigger should succeed without bubbling the network exception
        IncidentResponse response = incidentService.createIncident(
                communityId, residentUserId, "Rahul Sharma", "+91 98450 99887", request);

        assertNotNull(response);
        assertEquals(2001L, response.getId());
        assertEquals(EmergencyStatus.TRIGGERED, response.getStatus());
        assertEquals("Tower B", response.getTower());
        assertEquals("B-304", response.getFlatNumber());
        assertNotNull(response.getEscalationDeadline());
        assertFalse(response.getSlaBreached());

        // Verify WebSocket was attempted but safely handled
        verify(messagingTemplate).convertAndSend(eq("/topic/community/" + communityId + "/emergency"), any(Object.class));

        // Verify audit timeline recorded the trigger
        assertEquals(1, auditTimelineLog.size());
        EmergencyIncidentStatusHistory initialHistory = auditTimelineLog.get(0);
        assertNull(initialHistory.getFromStatus());
        assertEquals(EmergencyStatus.TRIGGERED, initialHistory.getToStatus());
        assertEquals(residentUserId, initialHistory.getChangedByUserId());
        assertEquals("RESIDENT", initialHistory.getChangedByRole());
    }

    @Test
    @DisplayName("Acknowledgement: Guard acknowledgement succeeds and updates SLA when network broadcast fails")
    void testAcknowledgementUnderNetworkFailure_BroadcastFails() {
        EmergencyIncident incident = EmergencyIncident.builder()
                .id(2002L)
                .incidentRef("INC-20261010-000002")
                .communityId(communityId)
                .reportedByUserId(residentUserId)
                .reporterName("Rahul Sharma")
                .emergencyType(EmergencyType.FIRE)
                .priority(EmergencyPriority.CRITICAL)
                .status(EmergencyStatus.TRIGGERED)
                .escalationDeadline(LocalDateTime.now().plusSeconds(60))
                .build();

        when(incidentRepository.findById(2002L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(EmergencyIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        // Simulate network failure on WebSocket broadcast
        doThrow(new MessagingException("Socket write timeout during broadcast"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // Guard acknowledges the emergency
        IncidentResponse ackResponse = incidentService.acknowledgeIncident(
                communityId, 2002L, guardUserId, "Guard Ramesh", "SECURITY_GUARD");

        assertNotNull(ackResponse);
        assertEquals(EmergencyStatus.ACKNOWLEDGED, ackResponse.getStatus());
        assertNotNull(incident.getAcknowledgedAt());
        assertEquals(guardUserId, incident.getAcknowledgedByUserId());
        assertNotNull(incident.getEscalationDeadline(), "Next SLA assignment deadline must be established");

        // Verify timeline has transition
        assertEquals(1, auditTimelineLog.size());
        EmergencyIncidentStatusHistory ackHistory = auditTimelineLog.get(0);
        assertEquals(EmergencyStatus.TRIGGERED, ackHistory.getFromStatus());
        assertEquals(EmergencyStatus.ACKNOWLEDGED, ackHistory.getToStatus());
        assertEquals(guardUserId, ackHistory.getChangedByUserId());
        assertEquals("SECURITY_GUARD", ackHistory.getChangedByRole());
        assertTrue(ackHistory.getComment().contains("Guard Ramesh"));
    }

    @Test
    @DisplayName("Escalation: Watchdog auto-escalates to Supervisor when network outage prevented guard acknowledgment")
    void testEscalationUnderNetworkFailure_SlaBreachDueToMissingAck() {
        LocalDateTime pastDeadline = LocalDateTime.now().minusSeconds(15);
        EmergencyIncident breachedIncident = EmergencyIncident.builder()
                .id(2003L)
                .incidentRef("INC-20261010-000003")
                .communityId(communityId)
                .reportedByUserId(residentUserId)
                .reporterName("Sneha Rao")
                .emergencyType(EmergencyType.FIRE)
                .priority(EmergencyPriority.CRITICAL)
                .status(EmergencyStatus.TRIGGERED)
                .currentEscalationLevel(EscalationLevel.LEVEL_1_SECURITY)
                .escalationDeadline(pastDeadline)
                .slaBreached(false)
                .build();

        when(incidentRepository.findBreachedIncidents(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(breachedIncident));
        when(incidentRepository.save(any(EmergencyIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        // Simulate network down during watchdog broadcast
        doThrow(new MessagingException("Connection reset by peer"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // Watchdog executes scheduled scan
        watchdogService.scanAndEscalateBreachedIncidents();

        // Verify auto-escalation state
        assertEquals(EscalationLevel.LEVEL_2_SUPERVISOR, breachedIncident.getCurrentEscalationLevel());
        assertEquals(EmergencyStatus.ESCALATED, breachedIncident.getStatus());
        assertTrue(breachedIncident.getSlaBreached());
        assertTrue(breachedIncident.getEscalationDeadline().isAfter(LocalDateTime.now()), "Deadline must be extended for supervisor");

        // Verify audit timeline
        assertEquals(1, auditTimelineLog.size());
        EmergencyIncidentStatusHistory escHistory = auditTimelineLog.get(0);
        assertEquals(EmergencyStatus.TRIGGERED, escHistory.getFromStatus());
        assertEquals(EmergencyStatus.ESCALATED, escHistory.getToStatus());
        assertEquals("SLA_WATCHDOG", escHistory.getChangedByName());
        assertEquals("SYSTEM", escHistory.getChangedByRole());
        assertTrue(escHistory.getComment().contains("SLA breached at LEVEL_1_SECURITY. Auto-escalated to LEVEL_2_SUPERVISOR"));
    }

    @Test
    @DisplayName("Cascaded Escalation: Multi-level escalation progression across persistent network failure (L1 -> L2 -> L3 -> L4)")
    void testCascadedEscalationProgressionUnderPersistentNetworkFailure() {
        EmergencyIncident incident = EmergencyIncident.builder()
                .id(2004L)
                .incidentRef("INC-20261010-000004")
                .communityId(communityId)
                .reportedByUserId(residentUserId)
                .emergencyType(EmergencyType.WATER_LEAKAGE)
                .priority(EmergencyPriority.CRITICAL)
                .status(EmergencyStatus.TRIGGERED)
                .currentEscalationLevel(EscalationLevel.LEVEL_1_SECURITY)
                .escalationDeadline(LocalDateTime.now().minusSeconds(30))
                .build();

        when(incidentRepository.findBreachedIncidents(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(incident));
        when(incidentRepository.save(any(EmergencyIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        // WebSocket is persistently offline
        doThrow(new MessagingException("Network unreachable"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // Cycle 1: Level 1 -> Level 2 (Supervisor)
        watchdogService.scanAndEscalateBreachedIncidents();
        assertEquals(EscalationLevel.LEVEL_2_SUPERVISOR, incident.getCurrentEscalationLevel());
        assertEquals(EmergencyStatus.ESCALATED, incident.getStatus());

        // Cycle 2: Level 2 -> Level 3 (Admin)
        incident.setEscalationDeadline(LocalDateTime.now().minusSeconds(1));
        watchdogService.scanAndEscalateBreachedIncidents();
        assertEquals(EscalationLevel.LEVEL_3_ADMIN, incident.getCurrentEscalationLevel());

        // Cycle 3: Level 3 -> Level 4 (External Emergency Services)
        incident.setEscalationDeadline(LocalDateTime.now().minusSeconds(1));
        watchdogService.scanAndEscalateBreachedIncidents();
        assertEquals(EscalationLevel.LEVEL_4_EXTERNAL, incident.getCurrentEscalationLevel());

        // Cycle 4: Max level reached -> deadline cleared, no infinite loop
        incident.setEscalationDeadline(LocalDateTime.now().minusSeconds(1));
        watchdogService.scanAndEscalateBreachedIncidents();
        assertEquals(EscalationLevel.LEVEL_4_EXTERNAL, incident.getCurrentEscalationLevel());
        assertNull(incident.getEscalationDeadline(), "Deadline cleared when maximum escalation reached");

        // Verify audit timeline contains all 3 escalation hops
        assertEquals(3, auditTimelineLog.size());
        assertTrue(auditTimelineLog.get(0).getComment().contains("Auto-escalated to LEVEL_2_SUPERVISOR"));
        assertTrue(auditTimelineLog.get(1).getComment().contains("Auto-escalated to LEVEL_3_ADMIN"));
        assertTrue(auditTimelineLog.get(2).getComment().contains("Auto-escalated to LEVEL_4_EXTERNAL"));
    }

    @Test
    @DisplayName("Manual Escalation: Operator manually escalates during communication breakdown")
    void testManualEscalationUnderNetworkFailure() {
        EmergencyIncident incident = EmergencyIncident.builder()
                .id(2005L)
                .incidentRef("INC-20261010-000005")
                .communityId(communityId)
                .reportedByUserId(residentUserId)
                .emergencyType(EmergencyType.LIFT_EMERGENCY)
                .status(EmergencyStatus.ACKNOWLEDGED)
                .currentEscalationLevel(EscalationLevel.LEVEL_1_SECURITY)
                .build();

        when(incidentRepository.findById(2005L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(EmergencyIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        doThrow(new MessagingException("Broadcast down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        IncidentResponse response = incidentService.escalateIncidentManually(
                communityId, 2005L, supervisorUserId, "Supervisor Vikram", "SUPERVISOR",
                EscalationLevel.LEVEL_3_ADMIN, "Elevator technician phone unreachable; contacting Otis directly");

        assertNotNull(response);
        assertEquals(EmergencyStatus.ESCALATED, response.getStatus());
        assertEquals(EscalationLevel.LEVEL_3_ADMIN, response.getCurrentEscalationLevel());
        assertTrue(response.getSlaBreached());

        assertEquals(1, auditTimelineLog.size());
        EmergencyIncidentStatusHistory hist = auditTimelineLog.get(0);
        assertEquals(EmergencyStatus.ACKNOWLEDGED, hist.getFromStatus());
        assertEquals(EmergencyStatus.ESCALATED, hist.getToStatus());
        assertEquals("SUPERVISOR", hist.getChangedByRole());
        assertTrue(hist.getComment().contains("Elevator technician phone unreachable"));
    }

    @Test
    @DisplayName("Audit Trail: Complete end-to-end lifecycle provenance and KPI report under persistent network failure")
    void testCompleteAuditTrailUnderPersistentNetworkFailure() {
        LocalDateTime t0 = LocalDateTime.now().minusMinutes(20);
        LocalDateTime t1 = t0.plusMinutes(2);
        LocalDateTime t2 = t0.plusMinutes(4);
        LocalDateTime t3 = t0.plusMinutes(8);
        LocalDateTime t4 = t0.plusMinutes(15);

        // Fail all broadcasts throughout the entire lifecycle
        doThrow(new MessagingException("Persistent network failure"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        EmergencyIncident incident = EmergencyIncident.builder()
                .id(2006L)
                .incidentRef("INC-20261010-000006")
                .communityId(communityId)
                .reportedByUserId(residentUserId)
                .reporterName("Vikram Mehta")
                .emergencyType(EmergencyType.MEDICAL)
                .priority(EmergencyPriority.CRITICAL)
                .status(EmergencyStatus.TRIGGERED)
                .createdAt(t0)
                .build();

        when(incidentRepository.findById(2006L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(EmergencyIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Initial trigger timeline log
        incidentService.logTimeline(2006L, communityId, null, EmergencyStatus.TRIGGERED,
                residentUserId, "Vikram Mehta", "RESIDENT", "Emergency raised via SOS");

        // 2. Watchdog auto-escalation due to comms delay
        incidentService.logTimeline(2006L, communityId, EmergencyStatus.TRIGGERED, EmergencyStatus.ESCALATED,
                null, "SLA_WATCHDOG", "SYSTEM", "SLA breached: auto-escalated to LEVEL_2_SUPERVISOR");

        // 3. Acknowledged once field comms restored
        incidentService.acknowledgeIncident(communityId, 2006L, guardUserId, "Guard Singh", "SECURITY_GUARD");
        incident.setAcknowledgedAt(t1);

        // 4. Responder on scene
        incidentService.markOnScene(communityId, 2006L, guardUserId, "Nurse Priya");
        incident.setOnSceneAt(t3);

        // 5. Emergency resolved
        incidentService.resolveIncident(communityId, 2006L, guardUserId, "Nurse Priya", "Patient administered first aid and stabilized");
        incident.setResolvedAt(t4);

        // Configure mock timeline retrieval for audit report
        when(timelineRepository.findByIncidentIdOrderByCreatedAtAsc(2006L)).thenReturn(auditTimelineLog);

        // Generate Audit Report
        IncidentAuditReportResponse auditReport = incidentService.generateAuditReport(communityId, 2006L);

        assertNotNull(auditReport);
        assertEquals(2006L, auditReport.getIncident().getId());
        assertEquals("INC-20261010-000006", auditReport.getIncident().getIncidentRef());
        assertNotNull(auditReport.getAuditSummary());
        assertTrue(auditReport.getAuditSummary().contains("INC-20261010-000006"));

        // Verify computed latency analytics KPIs
        assertNotNull(auditReport.getTimeToAcknowledgeSeconds());
        assertNotNull(auditReport.getTimeToOnSceneSeconds());
        assertNotNull(auditReport.getTotalResolutionTimeSeconds());

        // Verify that all 5 timeline transitions exist in order despite network failure
        assertEquals(5, auditReport.getTimeline().size());
        assertEquals(EmergencyStatus.TRIGGERED, auditReport.getTimeline().get(0).getToStatus());
        assertEquals(EmergencyStatus.ESCALATED, auditReport.getTimeline().get(1).getToStatus());
        assertEquals(EmergencyStatus.ACKNOWLEDGED, auditReport.getTimeline().get(2).getToStatus());
        assertEquals(EmergencyStatus.ON_SCENE, auditReport.getTimeline().get(3).getToStatus());
        assertEquals(EmergencyStatus.RESOLVED, auditReport.getTimeline().get(4).getToStatus());
    }
}
