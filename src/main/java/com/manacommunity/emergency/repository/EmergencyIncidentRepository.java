package com.manacommunity.emergency.repository;

import com.manacommunity.emergency.enums.EmergencyStatus;
import com.manacommunity.emergency.enums.EmergencyType;
import com.manacommunity.emergency.model.EmergencyIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmergencyIncidentRepository extends JpaRepository<EmergencyIncident, Long> {

    Optional<EmergencyIncident> findByIncidentRef(String incidentRef);

    Page<EmergencyIncident> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    Page<EmergencyIncident> findByCommunityIdAndReportedByUserIdOrderByCreatedAtDesc(Long communityId, Long userId, Pageable pageable);

    Page<EmergencyIncident> findByCommunityIdAndStatusInOrderByCreatedAtDesc(Long communityId, List<EmergencyStatus> statuses, Pageable pageable);

    List<EmergencyIncident> findByCommunityIdAndStatusIn(Long communityId, List<EmergencyStatus> statuses);

    @Query("SELECT i FROM EmergencyIncident i WHERE i.status IN :activeStatuses AND i.escalationDeadline <= :now")
    List<EmergencyIncident> findBreachedIncidents(
            @Param("activeStatuses") List<EmergencyStatus> activeStatuses,
            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(i) FROM EmergencyIncident i WHERE i.communityId = :cid AND i.status IN :activeStatuses")
    Long countActiveByCommunity(@Param("cid") Long communityId, @Param("activeStatuses") List<EmergencyStatus> activeStatuses);

    @Query("SELECT COUNT(i) FROM EmergencyIncident i WHERE i.communityId = :cid AND i.status = :status AND i.createdAt >= :since")
    Long countByCommunityAndStatusSince(@Param("cid") Long communityId, @Param("status") EmergencyStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT i.emergencyType, COUNT(i) FROM EmergencyIncident i WHERE i.communityId = :cid AND i.createdAt >= :since GROUP BY i.emergencyType")
    List<Object[]> countGroupedByType(@Param("cid") Long communityId, @Param("since") LocalDateTime since);
}
