package com.manacommunity.emergency.model;

import com.manacommunity.emergency.enums.ContactType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_contacts", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    /**
     * Null for community-wide verified emergency phonebook.
     * Set to resident userId for private family contacts.
     */
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 40)
    private ContactType contactType;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "relationship", length = 60)
    private String relationship;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "alternate_phone", length = 20)
    private String alternatePhone;

    @Column(name = "priority_order")
    @Builder.Default
    private Integer priorityOrder = 1;

    @Column(name = "is_24x7")
    @Builder.Default
    private Boolean is24x7 = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
