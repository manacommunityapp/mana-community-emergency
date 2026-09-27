package com.manacommunity.emergency.dto;

import com.manacommunity.emergency.enums.ResponderRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResponderResponse {
    private Long id;
    private Long communityId;
    private Long userId;
    private String name;
    private String phone;
    private ResponderRole role;
    private String badgeNumber;
    private Boolean isAvailable;
    private Boolean isOnDuty;
    private Integer currentActiveIncidents;
    private LocalDateTime createdAt;
}
