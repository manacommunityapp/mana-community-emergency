package com.manacommunity.emergency.dto;

import com.manacommunity.emergency.enums.EmergencyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IncidentStatusUpdateRequest {

    @NotNull
    private EmergencyStatus newStatus;

    private String comment;
}
