package com.manacommunity.emergency.dto;

import com.manacommunity.emergency.enums.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmergencyContactRequest {

    @NotNull
    private ContactType contactType;

    @NotBlank
    private String name;

    private String relationship;

    @NotBlank
    private String phone;

    private String alternatePhone;
    private Integer priorityOrder;
    private Boolean is24x7;
}
