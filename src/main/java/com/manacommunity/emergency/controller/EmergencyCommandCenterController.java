package com.manacommunity.emergency.controller;

import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.emergency.dto.CommandCenterDashboardResponse;
import com.manacommunity.emergency.dto.IncidentResponse;
import com.manacommunity.emergency.service.EmergencyCommandCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/emergency/command-center")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
public class EmergencyCommandCenterController {

    private final EmergencyCommandCenterService commandCenterService;

    /**
     * GET /api/emergency/command-center/dashboard
     * Full Command Center real-time statistics, capacity and active boards.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<CommandCenterDashboardResponse> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(commandCenterService.getDashboard(communityId));
    }

    /**
     * GET /api/emergency/command-center/active
     * Real-time list of all active emergencies.
     */
    @GetMapping("/active")
    public ResponseEntity<List<IncidentResponse>> getActiveIncidents(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(commandCenterService.getActiveIncidents(communityId));
    }
}
