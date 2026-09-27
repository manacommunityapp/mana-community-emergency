package com.manacommunity.emergency.controller;

import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.emergency.dto.ResponderRequest;
import com.manacommunity.emergency.dto.ResponderResponse;
import com.manacommunity.emergency.service.ResponderManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency/responders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SECURITY_GUARD', 'ADMIN', 'SUPER_ADMIN')")
public class EmergencyResponderController {

    private final ResponderManagementService responderService;

    /**
     * GET /api/emergency/responders
     * List all responders for the community.
     */
    @GetMapping
    public ResponseEntity<List<ResponderResponse>> getResponders(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(responderService.getResponders(communityId));
    }

    /**
     * GET /api/emergency/responders/available
     * List currently available and on-duty responders.
     */
    @GetMapping("/available")
    public ResponseEntity<List<ResponderResponse>> getAvailableResponders(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(responderService.getAvailableResponders(communityId));
    }

    /**
     * POST /api/emergency/responders
     * Register or update responder details (Admin / Security Supervisor).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ResponderResponse> registerResponder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ResponderRequest request) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(responderService.registerResponder(communityId, request));
    }

    /**
     * PUT /api/emergency/responders/{responderId}/duty
     * Toggle responder duty status (on/off duty).
     */
    @PutMapping("/{responderId}/duty")
    public ResponseEntity<ResponderResponse> toggleDuty(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long responderId,
            @RequestParam boolean onDuty) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(responderService.toggleDuty(communityId, responderId, onDuty));
    }
}
