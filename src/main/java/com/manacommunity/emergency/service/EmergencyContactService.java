package com.manacommunity.emergency.service;

import com.manacommunity.emergency.dto.EmergencyContactRequest;
import com.manacommunity.emergency.dto.EmergencyContactResponse;
import com.manacommunity.emergency.enums.ContactType;
import com.manacommunity.emergency.model.EmergencyContact;
import com.manacommunity.emergency.repository.EmergencyContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmergencyContactService {

    private final EmergencyContactRepository contactRepository;

    public List<EmergencyContactResponse> getMyContacts(Long communityId, Long userId) {
        return contactRepository.findByCommunityIdAndUserIdOrderByPriorityOrderAsc(communityId, userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<EmergencyContactResponse> getCommunityContacts(Long communityId) {
        return contactRepository.findByCommunityIdAndUserIdIsNullAndIsActiveTrueOrderByPriorityOrderAsc(communityId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmergencyContactResponse addPersonalContact(Long communityId, Long userId, EmergencyContactRequest request) {
        EmergencyContact contact = EmergencyContact.builder()
                .communityId(communityId)
                .userId(userId)
                .contactType(request.getContactType())
                .name(request.getName())
                .relationship(request.getRelationship())
                .phone(request.getPhone())
                .alternatePhone(request.getAlternatePhone())
                .priorityOrder(request.getPriorityOrder() != null ? request.getPriorityOrder() : 1)
                .is24x7(false)
                .isActive(true)
                .build();
        return toResponse(contactRepository.save(contact));
    }

    @Transactional
    public EmergencyContactResponse addCommunityContact(Long communityId, EmergencyContactRequest request) {
        EmergencyContact contact = EmergencyContact.builder()
                .communityId(communityId)
                .userId(null) // Community general contact
                .contactType(request.getContactType())
                .name(request.getName())
                .relationship(request.getRelationship())
                .phone(request.getPhone())
                .alternatePhone(request.getAlternatePhone())
                .priorityOrder(request.getPriorityOrder() != null ? request.getPriorityOrder() : 1)
                .is24x7(Boolean.TRUE.equals(request.getIs24x7()))
                .isActive(true)
                .build();
        return toResponse(contactRepository.save(contact));
    }

    @Transactional
    public void deleteContact(Long communityId, Long userId, Long contactId, boolean isAdmin) {
        EmergencyContact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found: " + contactId));

        if (!contact.getCommunityId().equals(communityId)) {
            throw new RuntimeException("Contact does not belong to this community");
        }
        if (!isAdmin && (contact.getUserId() == null || !contact.getUserId().equals(userId))) {
            throw new RuntimeException("Cannot delete contact of another user");
        }
        contactRepository.delete(contact);
    }

    private EmergencyContactResponse toResponse(EmergencyContact c) {
        return EmergencyContactResponse.builder()
                .id(c.getId())
                .communityId(c.getCommunityId())
                .userId(c.getUserId())
                .contactType(c.getContactType())
                .name(c.getName())
                .relationship(c.getRelationship())
                .phone(c.getPhone())
                .alternatePhone(c.getAlternatePhone())
                .priorityOrder(c.getPriorityOrder())
                .is24x7(c.getIs24x7())
                .isActive(c.getIsActive())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
