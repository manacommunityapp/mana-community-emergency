package com.manacommunity.emergency.dto;

import com.manacommunity.emergency.model.EmergencyAssignment;
import com.manacommunity.emergency.model.EmergencyClosureReport;
import com.manacommunity.emergency.model.EmergencyEvidence;
import com.manacommunity.emergency.model.EmergencyIncidentStatusHistory;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IncidentDetailResponse {
    private IncidentResponse incident;
    private List<EmergencyIncidentStatusHistory> timeline;
    private List<EmergencyAssignment> assignments;
    private List<EmergencyEvidence> evidence;
    private EmergencyClosureReport closureReport;
}
