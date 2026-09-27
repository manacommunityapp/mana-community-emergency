package com.manacommunity.emergency.repository;

import com.manacommunity.emergency.model.EmergencyEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyEvidenceRepository extends JpaRepository<EmergencyEvidence, Long> {

    List<EmergencyEvidence> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
}
