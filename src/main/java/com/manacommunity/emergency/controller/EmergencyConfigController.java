package com.manacommunity.emergency.controller;

import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.emergency.dto.EscalationConfigRequest;
import com.manacommunity.emergency.dto.EscalationConfigResponse;
import com.manacommunity.emergency.enums.EmergencyType;
import com.manacommunity.emergency.model.EmergencyEscalationConfig;
import com.manacommunity.emergency.repository.EmergencyEscalationConfigRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/emergency/config")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class EmergencyConfigController {

    private final EmergencyEscalationConfigRepository configRepository;

    /**
     * GET /api/emergency/config
     * Get all SLA and escalation configurations for the community.
     */
    @GetMapping
    public ResponseEntity<List<EscalationConfigResponse>> getConfigs(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(
                configRepository.findByCommunityId(communityId).stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList())
        );
    }

    /**
     * PUT /api/emergency/config
     * Configure SLA timeouts, primary responder role, and auto-escalation targets.
     */
    @PutMapping
    public ResponseEntity<EscalationConfigResponse> saveConfig(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EscalationConfigRequest request) {
        Long communityId = principal.getAppUser().getCommunity().getId();

        EmergencyEscalationConfig config = configRepository
                .findByCommunityIdAndEmergencyType(communityId, request.getEmergencyType())
                .orElseGet(() -> EmergencyEscalationConfig.builder()
                        .communityId(communityId)
                        .emergencyType(request.getEmergencyType())
                        .build());

        if (request.getAckTimeoutSeconds() != null) config.setAckTimeoutSeconds(request.getAckTimeoutSeconds());
        if (request.getAssignTimeoutSeconds() != null) config.setAssignTimeoutSeconds(request.getAssignTimeoutSeconds());
        if (request.getOnSceneTimeoutSeconds() != null) config.setOnSceneTimeoutSeconds(request.getOnSceneTimeoutSeconds());
        if (request.getAutoEscalate() != null) config.setAutoEscalate(request.getAutoEscalate());
        if (request.getPrimaryResponderRole() != null) config.setPrimaryResponderRole(request.getPrimaryResponderRole());
        if (request.getEscalationTargetRole() != null) config.setEscalationTargetRole(request.getEscalationTargetRole());
        if (request.getNotifyAdminOnTrigger() != null) config.setNotifyAdminOnTrigger(request.getNotifyAdminOnTrigger());

        return ResponseEntity.ok(toResponse(configRepository.save(config)));
    }

    /**
     * POST /api/emergency/config/seed
     * Seed default SLA configurations for all emergency types.
     */
    @PostMapping("/seed")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> seedConfigs(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        int created = 0;
        for (EmergencyType type : EmergencyType.values()) {
            if (configRepository.findByCommunityIdAndEmergencyType(communityId, type).isEmpty()) {
                EmergencyEscalationConfig config = EmergencyEscalationConfig.builder()
                        .communityId(communityId)
                        .emergencyType(type)
                        .ackTimeoutSeconds(120)
                        .assignTimeoutSeconds(300)
                        .onSceneTimeoutSeconds(600)
                        .autoEscalate(true)
                        .notifyAdminOnTrigger(true)
                        .build();
                configRepository.save(config);
                created++;
            }
        }
        return ResponseEntity.ok("Seeded " + created + " emergency SLA configs for community " + communityId);
    }

    private EscalationConfigResponse toResponse(EmergencyEscalationConfig c) {
        return EscalationConfigResponse.builder()
                .id(c.getId())
                .communityId(c.getCommunityId())
                .emergencyType(c.getEmergencyType())
                .ackTimeoutSeconds(c.getAckTimeoutSeconds())
                .assignTimeoutSeconds(c.getAssignTimeoutSeconds())
                .onSceneTimeoutSeconds(c.getOnSceneTimeoutSeconds())
                .autoEscalate(c.getAutoEscalate())
                .primaryResponderRole(c.getPrimaryResponderRole())
                .escalationTargetRole(c.getEscalationTargetRole())
                .notifyAdminOnTrigger(c.getNotifyAdminOnTrigger())
                .build();
    }
}
