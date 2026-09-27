package com.manacommunity.emergency.repository;

import com.manacommunity.emergency.enums.EmergencyType;
import com.manacommunity.emergency.model.EmergencyEscalationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmergencyEscalationConfigRepository extends JpaRepository<EmergencyEscalationConfig, Long> {

    Optional<EmergencyEscalationConfig> findByCommunityIdAndEmergencyType(Long communityId, EmergencyType emergencyType);

    List<EmergencyEscalationConfig> findByCommunityId(Long communityId);
}
