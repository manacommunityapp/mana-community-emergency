package com.manacommunity.emergency.repository;

import com.manacommunity.emergency.model.EmergencyIncidentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyIncidentStatusHistoryRepository extends JpaRepository<EmergencyIncidentStatusHistory, Long> {

    List<EmergencyIncidentStatusHistory> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
}
