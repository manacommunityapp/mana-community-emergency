package com.manacommunity.emergency.service;

import com.manacommunity.emergency.dto.ResponderRequest;
import com.manacommunity.emergency.dto.ResponderResponse;
import com.manacommunity.emergency.model.EmergencyResponder;
import com.manacommunity.emergency.repository.EmergencyResponderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResponderManagementService {

    private final EmergencyResponderRepository responderRepository;

    public List<ResponderResponse> getResponders(Long communityId) {
        return responderRepository.findByCommunityId(communityId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ResponderResponse> getAvailableResponders(Long communityId) {
        return responderRepository.findByCommunityIdAndIsAvailableTrueAndIsOnDutyTrue(communityId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ResponderResponse registerResponder(Long communityId, ResponderRequest request) {
        EmergencyResponder responder = responderRepository.findByCommunityIdAndUserId(communityId, request.getUserId())
                .orElseGet(() -> EmergencyResponder.builder()
                        .communityId(communityId)
                        .userId(request.getUserId())
                        .build());

        responder.setName(request.getName());
        responder.setPhone(request.getPhone());
        responder.setRole(request.getRole());
        responder.setBadgeNumber(request.getBadgeNumber());
        responder.setIsAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true);
        responder.setIsOnDuty(request.getIsOnDuty() != null ? request.getIsOnDuty() : true);

        return toResponse(responderRepository.save(responder));
    }

    @Transactional
    public ResponderResponse toggleDuty(Long communityId, Long responderId, boolean onDuty) {
        EmergencyResponder responder = responderRepository.findById(responderId)
                .orElseThrow(() -> new RuntimeException("Responder not found: " + responderId));
        if (!responder.getCommunityId().equals(communityId)) {
            throw new RuntimeException("Responder does not belong to this community");
        }
        responder.setIsOnDuty(onDuty);
        responder.setIsAvailable(onDuty);
        return toResponse(responderRepository.save(responder));
    }

    private ResponderResponse toResponse(EmergencyResponder r) {
        return ResponderResponse.builder()
                .id(r.getId())
                .communityId(r.getCommunityId())
                .userId(r.getUserId())
                .name(r.getName())
                .phone(r.getPhone())
                .role(r.getRole())
                .badgeNumber(r.getBadgeNumber())
                .isAvailable(r.getIsAvailable())
                .isOnDuty(r.getIsOnDuty())
                .currentActiveIncidents(r.getCurrentActiveIncidents())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
