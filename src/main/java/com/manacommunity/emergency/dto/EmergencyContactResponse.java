package com.manacommunity.emergency.dto;

import com.manacommunity.emergency.enums.ContactType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmergencyContactResponse {
    private Long id;
    private Long communityId;
    private Long userId; // null if community-level
    private ContactType contactType;
    private String name;
    private String relationship;
    private String phone; // masked if necessary in public context
    private String alternatePhone;
    private Integer priorityOrder;
    private Boolean is24x7;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
