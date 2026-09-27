package com.manacommunity.emergency.dto;

import com.manacommunity.emergency.enums.EmergencyPriority;
import com.manacommunity.emergency.enums.EmergencyType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateIncidentRequest {

    @NotNull
    private EmergencyType emergencyType;

    private EmergencyPriority priority; // Optional, defaults to CRITICAL for SOS/FIRE/MEDICAL, HIGH otherwise

    private String title;
    private String description;
    private Boolean isAnonymous;

    // Location
    private String tower;
    private String floor;
    private String flatNumber;
    private String commonAreaName;
    private Double latitude;
    private Double longitude;
    private String locationNotes;
}
