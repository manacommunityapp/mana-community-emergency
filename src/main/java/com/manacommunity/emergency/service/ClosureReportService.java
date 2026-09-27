package com.manacommunity.emergency.service;

import com.manacommunity.emergency.dto.ClosureReportRequest;
import com.manacommunity.emergency.dto.ClosureReportResponse;
import com.manacommunity.emergency.dto.ReviewClosureRequest;
import com.manacommunity.emergency.enums.EmergencyStatus;
import com.manacommunity.emergency.model.EmergencyClosureReport;
import com.manacommunity.emergency.model.EmergencyIncident;
import com.manacommunity.emergency.repository.EmergencyClosureReportRepository;
import com.manacommunity.emergency.repository.EmergencyIncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClosureReportService {

    private final EmergencyClosureReportRepository closureReportRepository;
    private final EmergencyIncidentRepository incidentRepository;
    private final IncidentManagementService incidentService;

    @Transactional
    public ClosureReportResponse submitClosureReport(Long communityId, Long incidentId, Long responderUserId,
                                                     String responderName, ClosureReportRequest request) {
        EmergencyIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + incidentId));

        if (!incident.getCommunityId().equals(communityId)) {
            throw new RuntimeException("Incident does not belong to this community");
        }

        EmergencyClosureReport report = EmergencyClosureReport.builder()
                .incidentId(incidentId)
                .communityId(communityId)
                .submittedByUserId(responderUserId)
                .submittedByName(responderName)
                .resolutionSummary(request.getResolutionSummary())
                .rootCause(request.getRootCause())
                .actionTaken(request.getActionTaken())
                .preventativeMeasures(request.getPreventativeMeasures())
                .arrivalTime(request.getArrivalTime())
                .resolvedTime(request.getResolvedTime() != null ? request.getResolvedTime() : LocalDateTime.now())
                .isApproved(true)
                .build();

        report = closureReportRepository.save(report);

        // Mark incident as RESOLVED if not already
        if (incident.getStatus() != EmergencyStatus.RESOLVED && incident.getStatus() != EmergencyStatus.CLOSED) {
            incident.setStatus(EmergencyStatus.RESOLVED);
            incident.setResolvedAt(LocalDateTime.now());
            incidentRepository.save(incident);

            incidentService.logTimeline(incidentId, communityId, incident.getStatus(), EmergencyStatus.RESOLVED,
                    responderUserId, responderName, "RESPONDER",
                    "Submitted closure report: " + request.getResolutionSummary());
        }

        return toResponse(report);
    }

    @Transactional
    public ClosureReportResponse reviewClosureReport(Long communityId, Long incidentId, Long adminUserId,
                                                     ReviewClosureRequest request) {
        EmergencyClosureReport report = closureReportRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new RuntimeException("Closure report not found for incident: " + incidentId));

        if (!report.getCommunityId().equals(communityId)) {
            throw new RuntimeException("Report does not belong to this community");
        }

        report.setAdminReviewedByUserId(adminUserId);
        report.setAdminReviewedAt(LocalDateTime.now());
        report.setAdminNotes(request.getAdminNotes());
        report.setIsApproved(Boolean.TRUE.equals(request.getIsApproved()));

        report = closureReportRepository.save(report);

        // If approved, mark incident CLOSED
        if (Boolean.TRUE.equals(request.getIsApproved())) {
            incidentRepository.findById(incidentId).ifPresent(i -> {
                i.setStatus(EmergencyStatus.CLOSED);
                i.setClosedAt(LocalDateTime.now());
                incidentRepository.save(i);

                incidentService.logTimeline(incidentId, communityId, EmergencyStatus.RESOLVED, EmergencyStatus.CLOSED,
                        adminUserId, "Admin", "ADMIN", "Closure report approved & incident closed.");
            });
        }

        return toResponse(report);
    }

    public ClosureReportResponse getClosureReport(Long communityId, Long incidentId) {
        EmergencyClosureReport report = closureReportRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new RuntimeException("Closure report not found for incident: " + incidentId));
        if (!report.getCommunityId().equals(communityId)) {
            throw new RuntimeException("Report does not belong to this community");
        }
        return toResponse(report);
    }

    private ClosureReportResponse toResponse(EmergencyClosureReport r) {
        return ClosureReportResponse.builder()
                .id(r.getId())
                .incidentId(r.getIncidentId())
                .communityId(r.getCommunityId())
                .submittedByUserId(r.getSubmittedByUserId())
                .submittedByName(r.getSubmittedByName())
                .resolutionSummary(r.getResolutionSummary())
                .rootCause(r.getRootCause())
                .actionTaken(r.getActionTaken())
                .preventativeMeasures(r.getPreventativeMeasures())
                .arrivalTime(r.getArrivalTime())
                .resolvedTime(r.getResolvedTime())
                .submittedAt(r.getSubmittedAt())
                .adminReviewedByUserId(r.getAdminReviewedByUserId())
                .adminReviewedAt(r.getAdminReviewedAt())
                .adminNotes(r.getAdminNotes())
                .isApproved(r.getIsApproved())
                .build();
    }
}
