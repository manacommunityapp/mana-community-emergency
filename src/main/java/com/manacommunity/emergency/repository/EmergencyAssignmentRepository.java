package com.manacommunity.emergency.repository;

import com.manacommunity.emergency.model.EmergencyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyAssignmentRepository extends JpaRepository<EmergencyAssignment, Long> {

    List<EmergencyAssignment> findByIncidentId(Long incidentId);

    List<EmergencyAssignment> findByResponderId(Long responderId);
}
