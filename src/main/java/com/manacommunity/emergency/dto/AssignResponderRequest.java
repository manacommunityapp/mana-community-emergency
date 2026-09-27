package com.manacommunity.emergency.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignResponderRequest {

    @NotNull
    private Long responderId;

    private Boolean isLead;
    private String notes;
}
