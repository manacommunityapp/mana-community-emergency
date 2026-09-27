package com.manacommunity.emergency.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClosureReportRequest {

    @NotBlank
    private String resolutionSummary;

    private String rootCause;

    @NotBlank
    private String actionTaken;

    private String preventativeMeasures;
    private LocalDateTime arrivalTime;
    private LocalDateTime resolvedTime;
}
