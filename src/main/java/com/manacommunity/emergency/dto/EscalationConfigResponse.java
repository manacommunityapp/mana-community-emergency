package com.manacommunity.emergency.dto;

import com.manacommunity.emergency.enums.EmergencyType;
import com.manacommunity.emergency.enums.ResponderRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EscalationConfigResponse {
    private Long id;
    private Long communityId;
    private EmergencyType emergencyType;
    private Integer ackTimeoutSeconds;
    private Integer assignTimeoutSeconds;
    private Integer onSceneTimeoutSeconds;
    private Boolean autoEscalate;
    private ResponderRole primaryResponderRole;
    private ResponderRole escalationTargetRole;
    private Boolean notifyAdminOnTrigger;
}
