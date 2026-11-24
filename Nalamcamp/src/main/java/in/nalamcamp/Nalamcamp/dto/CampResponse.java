package in.nalamcamp.Nalamcamp.dto;

import in.nalamcamp.Nalamcamp.Camp;
import in.nalamcamp.Nalamcamp.CampSession;
import in.nalamcamp.Nalamcamp.Organizer;
import in.nalamcamp.Nalamcamp.controller.CampSessionController.SessionResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CampResponse(
        Long id,
        String name,
        String organizerName,
        Organizer.OrganizerType organizerType,
        LocalDate campDate,
        String area,
        String venue,
        String status,
        String verificationSource,
        String rejectionReason,
        LocalDateTime reviewedAt,
        List<SessionResponse> services
) {
    public static CampResponse from(Camp camp, List<CampSession> sessions) {
        List<SessionResponse> sessionResponses = sessions == null ? List.of() :
                sessions.stream().map(SessionResponse::from).toList();

        return new CampResponse(
                camp.getId(),
                camp.getName(),
                camp.getOrganizerName(),
                camp.getOrganizerType(),
                camp.getCampDate(),
                camp.getArea(),
                camp.getVenue(),
                camp.getStatus(),
                camp.getVerificationSource(),
                camp.getRejectionReason(),
                camp.getReviewedAt(),
                sessionResponses
        );
    }
}
