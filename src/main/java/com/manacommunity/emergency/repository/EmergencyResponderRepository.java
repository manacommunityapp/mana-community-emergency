package com.manacommunity.emergency.repository;

import com.manacommunity.emergency.enums.ResponderRole;
import com.manacommunity.emergency.model.EmergencyResponder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmergencyResponderRepository extends JpaRepository<EmergencyResponder, Long> {

    Optional<EmergencyResponder> findByCommunityIdAndUserId(Long communityId, Long userId);

    List<EmergencyResponder> findByCommunityIdAndIsAvailableTrueAndIsOnDutyTrue(Long communityId);

    List<EmergencyResponder> findByCommunityIdAndRoleAndIsAvailableTrueAndIsOnDutyTrue(Long communityId, ResponderRole role);

    List<EmergencyResponder> findByCommunityId(Long communityId);
}
