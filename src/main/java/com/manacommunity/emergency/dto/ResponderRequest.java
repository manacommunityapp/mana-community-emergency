package com.manacommunity.emergency.dto;

import com.manacommunity.emergency.enums.ResponderRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResponderRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    @NotNull
    private ResponderRole role;

    private String badgeNumber;
    private Boolean isAvailable;
    private Boolean isOnDuty;
}
