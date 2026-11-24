package in.nalamcamp.Nalamcamp.controller;

import in.nalamcamp.Nalamcamp.CampSession;
import in.nalamcamp.Nalamcamp.dto.CampSessionRequest;
import in.nalamcamp.Nalamcamp.service.CampSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CampSessionController {

    private final CampSessionService sessionService;

    public CampSessionController(CampSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/camps/{campId}/services")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse createSession(
            @PathVariable("campId") Long campId,
            @Valid @RequestBody CampSessionRequest request,
            HttpServletRequest httpRequest) {

        return SessionResponse.from(sessionService.createSession(campId, request, httpRequest));
    }

    @GetMapping("/camps/{campId}/services")
    public List<SessionResponse> getSessionsByCamp(
            @PathVariable("campId") Long campId) {

        return sessionService.getSessionsByCamp(campId)
                .stream()
                .map(SessionResponse::from)
                .toList();
    }

    @PutMapping("/services/{sessionId}")
    public SessionResponse updateSession(
            @PathVariable("sessionId") Long sessionId,
            @Valid @RequestBody CampSessionRequest request,
            HttpServletRequest httpRequest) {

        return SessionResponse.from(sessionService.updateSession(sessionId, request, httpRequest));
    }

    @PatchMapping("/services/{sessionId}")
    public SessionResponse patchSession(
            @PathVariable("sessionId") Long sessionId,
            @RequestBody(required = false) CampSessionRequest request,
            HttpServletRequest httpRequest) {

        if (request != null && request.serviceName() != null) {
            return SessionResponse.from(sessionService.updateSession(sessionId, request, httpRequest));
        } else {
            return SessionResponse.from(sessionService.closeSession(sessionId, httpRequest));
        }
    }

    public record SessionResponse(
            Long id,
            String serviceName,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal fee,
            String itemsToBring,
            CampSession.BookingMode bookingMode,
            int onlineCapacity,
            int onlineReserved,
            int availableTokens,
            CampSession.Status status
    ) {
        public static SessionResponse from(CampSession session) {
            return new SessionResponse(
                    session.getId(),
                    session.getServiceName(),
                    session.getStartTime(),
                    session.getEndTime(),
                    session.getFee(),
                    session.getItemsToBring(),
                    session.getBookingMode(),
                    session.getOnlineCapacity(),
                    session.getOnlineReserved(),
                    session.getAvailableTokens(),
                    session.getStatus()
            );
        }
    }
}
