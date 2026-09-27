package com.manacommunity.emergency.repository;

import com.manacommunity.emergency.model.EmergencyClosureReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmergencyClosureReportRepository extends JpaRepository<EmergencyClosureReport, Long> {

    Optional<EmergencyClosureReport> findByIncidentId(Long incidentId);
}
