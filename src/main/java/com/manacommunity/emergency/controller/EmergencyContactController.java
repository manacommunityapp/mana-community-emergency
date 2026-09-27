package com.manacommunity.emergency.controller;

import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.emergency.dto.EmergencyContactRequest;
import com.manacommunity.emergency.dto.EmergencyContactResponse;
import com.manacommunity.emergency.service.EmergencyContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency/contacts")
@RequiredArgsConstructor
public class EmergencyContactController {

    private final EmergencyContactService contactService;

    /**
     * GET /api/emergency/contacts/my
     * List user's private trusted family contacts.
     */
    @GetMapping("/my")
    public ResponseEntity<List<EmergencyContactResponse>> getMyContacts(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(contactService.getMyContacts(communityId, principal.getId()));
    }

    /**
     * POST /api/emergency/contacts/my
     * Add a personal trusted family emergency contact.
     */
    @PostMapping("/my")
    public ResponseEntity<EmergencyContactResponse> addPersonalContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmergencyContactRequest request) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(contactService.addPersonalContact(communityId, principal.getId(), request));
    }

    /**
     * GET /api/emergency/contacts/community
     * List verified community emergency phonebook (Security desk, 24x7 control room, fire, ambulance).
     */
    @GetMapping("/community")
    public ResponseEntity<List<EmergencyContactResponse>> getCommunityContacts(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(contactService.getCommunityContacts(communityId));
    }

    /**
     * POST /api/emergency/contacts/community
     * Add a community-level emergency phonebook number (Admin only).
     */
    @PostMapping("/community")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<EmergencyContactResponse> addCommunityContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmergencyContactRequest request) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        return ResponseEntity.ok(contactService.addCommunityContact(communityId, request));
    }

    /**
     * DELETE /api/emergency/contacts/{id}
     * Delete an emergency contact (owner or admin).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long communityId = principal.getAppUser().getCommunity().getId();
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        contactService.deleteContact(communityId, principal.getId(), id, isAdmin);
        return ResponseEntity.noContent().build();
    }
}
