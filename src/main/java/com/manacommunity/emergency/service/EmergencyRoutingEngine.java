package com.manacommunity.emergency.service;

import com.manacommunity.emergency.enums.EmergencyType;
import com.manacommunity.emergency.enums.ResponderRole;
import com.manacommunity.emergency.model.EmergencyEscalationConfig;
import com.manacommunity.emergency.model.EmergencyIncident;
import com.manacommunity.emergency.model.EmergencyResponder;
import com.manacommunity.emergency.repository.EmergencyEscalationConfigRepository;
import com.manacommunity.emergency.repository.EmergencyResponderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Emergency-specific routing engine.
 * Selects primary and secondary responder groups based on EmergencyType.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyRoutingEngine {

    private final EmergencyEscalationConfigRepository configRepository;
    private final EmergencyResponderRepository responderRepository;

    /**
     * Resolve the primary responder role for an emergency type.
     */
    public ResponderRole resolvePrimaryRole(Long communityId, EmergencyType emergencyType) {
        return configRepository.findByCommunityIdAndEmergencyType(communityId, emergencyType)
                .map(EmergencyEscalationConfig::getPrimaryResponderRole)
                .orElseGet(() -> defaultPrimaryRole(emergencyType));
    }

    /**
     * Resolve the secondary / escalation target role.
     */
    public ResponderRole resolveEscalationRole(Long communityId, EmergencyType emergencyType) {
        return configRepository.findByCommunityIdAndEmergencyType(communityId, emergencyType)
                .map(EmergencyEscalationConfig::getEscalationTargetRole)
                .orElse(ResponderRole.SECURITY_SUPERVISOR);
    }

    /**
     * Find available responders matching the resolved role for dispatch.
     */
    public List<EmergencyResponder> findEligibleResponders(Long communityId, EmergencyType emergencyType) {
        ResponderRole primaryRole = resolvePrimaryRole(communityId, emergencyType);
        List<EmergencyResponder> available = responderRepository
                .findByCommunityIdAndRoleAndIsAvailableTrueAndIsOnDutyTrue(communityId, primaryRole);

        if (available.isEmpty()) {
            // Fallback to security guards if specialized responder is not on duty
            log.warn("No available {} for community {}. Falling back to SECURITY_GUARD", primaryRole, communityId);
            available = responderRepository
                    .findByCommunityIdAndRoleAndIsAvailableTrueAndIsOnDutyTrue(communityId, ResponderRole.SECURITY_GUARD);
        }
        return available;
    }

    private ResponderRole defaultPrimaryRole(EmergencyType emergencyType) {
        return switch (emergencyType) {
            case SOS_GENERAL, SECURITY, WOMEN_SAFETY, CHILD_SAFETY -> ResponderRole.SECURITY_GUARD;
            case MEDICAL -> ResponderRole.MEDICAL_FIRST_RESPONDER;
            case FIRE -> ResponderRole.SECURITY_GUARD;
            case LIFT_EMERGENCY, POWER_OUTAGE -> ResponderRole.ELECTRICIAN;
            case WATER_LEAKAGE -> ResponderRole.PLUMBER;
            default -> ResponderRole.SECURITY_GUARD;
        };
    }
}
