package com.manacommunity.emergency.repository;

import com.manacommunity.emergency.model.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    List<EmergencyContact> findByCommunityIdAndUserIdOrderByPriorityOrderAsc(Long communityId, Long userId);

    List<EmergencyContact> findByCommunityIdAndUserIdIsNullAndIsActiveTrueOrderByPriorityOrderAsc(Long communityId);

    List<EmergencyContact> findByCommunityIdOrderByPriorityOrderAsc(Long communityId);
}
