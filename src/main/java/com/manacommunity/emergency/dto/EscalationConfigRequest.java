package com.manacommunity.emergency.dto;

import com.manacommunity.emergency.enums.EmergencyType;
import com.manacommunity.emergency.enums.ResponderRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EscalationConfigRequest {

    @NotNull
    private EmergencyType emergencyType;

    private Integer ackTimeoutSeconds;
    private Integer assignTimeoutSeconds;
    private Integer onSceneTimeoutSeconds;
    private Boolean autoEscalate;
    private ResponderRole primaryResponderRole;
    private ResponderRole escalationTargetRole;
    private Boolean notifyAdminOnTrigger;
}
