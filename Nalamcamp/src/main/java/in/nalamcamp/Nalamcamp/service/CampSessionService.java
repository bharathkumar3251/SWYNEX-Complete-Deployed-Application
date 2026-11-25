package in.nalamcamp.Nalamcamp.service;

import in.nalamcamp.Nalamcamp.Camp;
import in.nalamcamp.Nalamcamp.CampSession;
import in.nalamcamp.Nalamcamp.Staff;
import in.nalamcamp.Nalamcamp.dto.CampSessionRequest;
import in.nalamcamp.Nalamcamp.repository.CampRepository;
import in.nalamcamp.Nalamcamp.repository.CampSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CampSessionService {

    private final CampRepository campRepository;
    private final CampSessionRepository sessionRepository;
    private final StaffAccessService staffAccessService;

    public CampSessionService(CampRepository campRepository,
                              CampSessionRepository sessionRepository,
                              StaffAccessService staffAccessService) {
        this.campRepository = campRepository;
        this.sessionRepository = sessionRepository;
        this.staffAccessService = staffAccessService;
    }

    @Transactional
    public CampSession createSession(Long campId, CampSessionRequest request) {
        Camp camp = campRepository.findById(campId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Camp not found: " + campId));

        CampSession session = new CampSession();
        session.setCamp(camp);
        copyDetails(session, request);

        return sessionRepository.save(session);
    }

    @Transactional
    public CampSession createSession(Long campId, CampSessionRequest request, HttpServletRequest httpRequest) {
        Staff staff = staffAccessService.requireRole(httpRequest, Staff.Role.ORGANIZER);
        Camp camp = campRepository.findById(campId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Camp not found: " + campId));

        staffAccessService.checkCampOwnership(staff, camp);

        CampSession session = new CampSession();
        session.setCamp(camp);
        copyDetails(session, request);

        return sessionRepository.save(session);
    }

    public List<CampSession> getSessionsByCamp(Long campId) {
        if (!campRepository.existsById(campId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Camp not found: " + campId);
        }
        return sessionRepository.findByCamp_IdOrderByStartTimeAsc(campId);
    }

    @Transactional
    public CampSession updateSession(Long sessionId, CampSessionRequest request) {
        CampSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Counter not found: " + sessionId));

        if (request.onlineCapacity() < session.getOnlineReserved()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Online capacity cannot be lower than the number of reserved tokens");
        }

        copyDetails(session, request);
        return sessionRepository.save(session);
    }

    @Transactional
    public CampSession updateSession(Long sessionId, CampSessionRequest request, HttpServletRequest httpRequest) {
        Staff staff = staffAccessService.requireRole(httpRequest, Staff.Role.ORGANIZER);
        CampSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Counter not found: " + sessionId));

        staffAccessService.checkCampOwnership(staff, session.getCamp());

        if (request.onlineCapacity() < session.getOnlineReserved()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Online capacity cannot be lower than the number of reserved tokens");
        }

        copyDetails(session, request);
        return sessionRepository.save(session);
    }

    @Transactional
    public CampSession closeSession(Long sessionId, HttpServletRequest httpRequest) {
        Staff staff = staffAccessService.requireRole(httpRequest, Staff.Role.ORGANIZER);
        CampSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Counter not found: " + sessionId));

        staffAccessService.checkCampOwnership(staff, session.getCamp());

        session.setStatus(CampSession.Status.CLOSED);
        return sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Long sessionId, HttpServletRequest httpRequest) {
        Staff staff = staffAccessService.requireRole(httpRequest, Staff.Role.ORGANIZER);
        CampSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Counter not found: " + sessionId));

        staffAccessService.checkCampOwnership(staff, session.getCamp());
        if (!session.getCamp().isDraft()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can only delete counters on draft camps");
        }
        sessionRepository.delete(session);
    }

    private void copyDetails(CampSession session, CampSessionRequest request) {
        session.setServiceName(request.serviceName().strip());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setFee(request.fee());
        session.setItemsToBring(request.itemsToBring());
        session.setBookingMode(request.bookingMode());
        session.setOnlineCapacity(request.onlineCapacity());
    }
}
