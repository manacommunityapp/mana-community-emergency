package com.manacommunity.emergency.controller;

import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.emergency.dto.*;
import com.manacommunity.emergency.enums.EscalationLevel;
import com.manacommunity.emergency.service.ClosureReportService;
import com.manacommunity.emergency.service.IncidentManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency/incidents")
@RequiredArgsConstructor
public class EmergencyIncidentController {

    private final IncidentManagementService incidentService;
    private final ClosureReportService closureService;

    /**
     * POST /api/emergency/incidents
     * One-tap or 2-second hold SOS / Emergency trigger.
     */
    @PostMapping
    public ResponseEntity<IncidentResponse> triggerEmergency(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateIncidentRequest request) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        IncidentResponse response = incidentService.createIncident(
                communityId,
                principal.getId(),
                principal.getAppUser().getFullName(),
                principal.getAppUser().getPhone(),
                request
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/emergency/incidents/my
     * Resident's own active/past incidents.
     */
    @GetMapping("/my")
    public ResponseEntity<Page<IncidentResponse>> getMyIncidents(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.getMyIncidents(communityId, principal.getId(), pageable));
    }

    /**
     * GET /api/emergency/incidents/{id}
     * Get incident detail with full timeline, assignments and evidence.
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncidentDetailResponse> getIncidentDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.getIncidentDetail(communityId, id));
    }

    /**
     * PUT /api/emergency/incidents/{id}/status
     * Update incident status (e.g. ACKNOWLEDGE, ON_SCENE, RESOLVED, CLOSED).
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<IncidentResponse> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody IncidentStatusUpdateRequest request) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.updateStatus(
                communityId,
                id,
                principal.getId(),
                principal.getAppUser().getFullName(),
                principal.getAppUser().getRole(),
                request
        ));
    }

    /**
     * POST /api/emergency/incidents/{id}/assign
     * Assign a responder team/guard to an incident.
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<IncidentResponse> assignResponder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignResponderRequest request) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.assignResponder(
                communityId,
                id,
                principal.getId(),
                principal.getAppUser().getFullName(),
                request
        ));
    }

    /**
     * POST /api/emergency/incidents/{id}/auto-dispatch
     * Automatically dispatch eligible on-duty responder based on emergency type and current load.
     */
    @PostMapping("/{id}/auto-dispatch")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<IncidentResponse> autoDispatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.autoDispatch(
                communityId,
                id,
                principal.getId(),
                principal.getAppUser().getFullName()
        ));
    }

    /**
     * POST /api/emergency/incidents/{id}/acknowledge
     * Security Dispatcher / Guard acknowledges the emergency.
     */
    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<IncidentResponse> acknowledgeIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.acknowledgeIncident(
                communityId,
                id,
                principal.getId(),
                principal.getAppUser().getFullName(),
                principal.getAppUser().getRole()
        ));
    }

    /**
     * POST /api/emergency/incidents/{id}/eta
     * Responder accepts dispatch and provides Estimated Time of Arrival (minutes).
     * Transitions incident to RESPONDER_EN_ROUTE.
     */
    @PostMapping("/{id}/eta")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<IncidentResponse> setResponderEta(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam int etaMinutes) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.responderAcceptAndSetEta(
                communityId,
                id,
                principal.getId(),
                principal.getAppUser().getFullName(),
                etaMinutes
        ));
    }

    /**
     * POST /api/emergency/incidents/{id}/on-scene
     * Responder marks arrival on scene.
     */
    @PostMapping("/{id}/on-scene")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<IncidentResponse> markOnScene(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.markOnScene(
                communityId,
                id,
                principal.getId(),
                principal.getAppUser().getFullName()
        ));
    }

    /**
     * POST /api/emergency/incidents/{id}/under-action
     * Responder marks active intervention underway.
     */
    @PostMapping("/{id}/under-action")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<IncidentResponse> markUnderAction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String notes) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.markUnderAction(
                communityId,
                id,
                principal.getId(),
                principal.getAppUser().getFullName(),
                notes
        ));
    }

    /**
     * POST /api/emergency/incidents/{id}/resolve
     * Responder or Dispatcher marks emergency resolved.
     */
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<IncidentResponse> resolveIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String notes) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.resolveIncident(
                communityId,
                id,
                principal.getId(),
                principal.getAppUser().getFullName(),
                notes
        ));
    }

    /**
     * POST /api/emergency/incidents/{id}/escalate
     * Manual escalation to higher command authority.
     */
    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<IncidentResponse> escalateIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) EscalationLevel targetLevel,
            @RequestParam String reason) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.escalateIncidentManually(
                communityId,
                id,
                principal.getId(),
                principal.getAppUser().getFullName(),
                principal.getAppUser().getRole(),
                targetLevel,
                reason
        ));
    }

    /**
     * GET /api/emergency/incidents/{id}/audit
     * Full immutable audit report with Time-to-Ack, Time-to-Scene, and SLA metrics.
     */
    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<IncidentAuditReportResponse> getAuditReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.generateAuditReport(communityId, id));
    }

    /**
     * POST /api/emergency/incidents/{id}/cancel
     * Cancel an emergency or report a false alarm.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<IncidentResponse> cancelIncident(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestParam(defaultValue = "false") boolean isFalseAlarm) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(incidentService.cancelIncident(
                communityId,
                id,
                principal.getId(),
                principal.getAppUser().getFullName(),
                reason,
                isFalseAlarm
        ));
    }

    /**
     * POST /api/emergency/incidents/{id}/closure
     * Submit a formal closure report.
     */
    @PostMapping("/{id}/closure")
    @PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ClosureReportResponse> submitClosureReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ClosureReportRequest request) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(closureService.submitClosureReport(
                communityId,
                id,
                principal.getId(),
                principal.getAppUser().getFullName(),
                request
        ));
    }

    /**
     * POST /api/emergency/incidents/{id}/closure/review
     * Admin reviews and approves closure report.
     */
    @PostMapping("/{id}/closure/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ClosureReportResponse> reviewClosureReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody ReviewClosureRequest request) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(closureService.reviewClosureReport(
                communityId,
                id,
                principal.getId(),
                request
        ));
    }
}
