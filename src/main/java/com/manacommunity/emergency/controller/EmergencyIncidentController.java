package com.manacommunity.emergency.controller;

import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.emergency.dto.*;
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
