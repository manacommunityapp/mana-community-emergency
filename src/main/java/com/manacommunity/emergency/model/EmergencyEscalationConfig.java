package com.manacommunity.emergency.model;

import com.manacommunity.emergency.enums.EmergencyType;
import com.manacommunity.emergency.enums.ResponderRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "emergency_escalation_configs", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyEscalationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "emergency_type", nullable = false, length = 40)
    private EmergencyType emergencyType;

    @Column(name = "ack_timeout_seconds", nullable = false)
    @Builder.Default
    private Integer ackTimeoutSeconds = 120; // 2 minutes

    @Column(name = "assign_timeout_seconds", nullable = false)
    @Builder.Default
    private Integer assignTimeoutSeconds = 300; // 5 minutes

    @Column(name = "on_scene_timeout_seconds", nullable = false)
    @Builder.Default
    private Integer onSceneTimeoutSeconds = 600; // 10 minutes

    @Column(name = "auto_escalate")
    @Builder.Default
    private Boolean autoEscalate = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_responder_role", length = 40)
    @Builder.Default
    private ResponderRole primaryResponderRole = ResponderRole.SECURITY_GUARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_target_role", length = 40)
    @Builder.Default
    private ResponderRole escalationTargetRole = ResponderRole.SECURITY_SUPERVISOR;

    @Column(name = "notify_admin_on_trigger")
    @Builder.Default
    private Boolean notifyAdminOnTrigger = true;
}
