package in.nalamcamp.Nalamcamp.service;

import in.nalamcamp.Nalamcamp.Booking;
import in.nalamcamp.Nalamcamp.Camp;
import in.nalamcamp.Nalamcamp.CampSession;
import in.nalamcamp.Nalamcamp.Staff;
import in.nalamcamp.Nalamcamp.dto.AttendanceResponse;
import in.nalamcamp.Nalamcamp.dto.BookingRequest;
import in.nalamcamp.Nalamcamp.dto.BookingResponse;
import in.nalamcamp.Nalamcamp.dto.WalkInRequest;
import in.nalamcamp.Nalamcamp.repository.BookingRepository;
import in.nalamcamp.Nalamcamp.repository.CampRepository;
import in.nalamcamp.Nalamcamp.repository.CampSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CampSessionRepository sessionRepository;
    private final CampRepository campRepository;
    private final StaffAccessService staffAccessService;

    public BookingService(BookingRepository bookingRepository,
                          CampSessionRepository sessionRepository,
                          CampRepository campRepository,
                          StaffAccessService staffAccessService) {
        this.bookingRepository = bookingRepository;
        this.sessionRepository = sessionRepository;
        this.campRepository = campRepository;
        this.staffAccessService = staffAccessService;
    }

    @Transactional
    public BookingResponse reserveToken(Long sessionId, BookingRequest request) {
        if (request.visitorName() == null || request.visitorName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Visitor name is required");
        }

        String idempotencyKey = (request.idempotencyKey() != null && !request.idempotencyKey().isBlank())
                ? request.idempotencyKey().strip() : null;

        if (idempotencyKey != null) {
            Optional<Booking> existingOpt = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOpt.isPresent()) {
                Booking existing = existingOpt.get();
                boolean sameVisitor = existing.getVisitorName().equalsIgnoreCase(request.visitorName().strip());
                boolean sameSession = existing.getSession().getId().equals(sessionId);

                if (sameVisitor && sameSession) {
                    return BookingResponse.from(existing);
                } else {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Idempotency key already used for a different booking");
                }
            }
        }

        CampSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Service counter not found: " + sessionId));

        Camp camp = session.getCamp();

        if (camp.isCancelled()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Camp has been cancelled. New bookings are rejected.");
        }

        if (!camp.isApproved()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Camp is not approved for bookings.");
        }

        if (camp.getCampDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Camp date has already passed.");
        }

        if (!session.isOpen()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This service counter is closed.");
        }

        if (!session.allowsOnlineBooking()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This service is available for walk-in only.");
        }

        if (session.getOnlineReserved() >= session.getOnlineCapacity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Online booking is full. Walk-in details: Service accepts walk-ins from "
                            + session.getStartTime() + " to " + session.getEndTime() + " if space permits.");
        }

        session.setOnlineReserved(session.getOnlineReserved() + 1);
        sessionRepository.save(session);

        String ref = "BKG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String token = "TK-" + sessionId + "-" + String.format("%03d", session.getOnlineReserved());

        Booking booking = new Booking();
        booking.setBookingReference(ref);
        booking.setTokenNumber(token);
        booking.setSession(session);
        booking.setVisitorName(request.visitorName().strip());
        if (request.phone() != null && !request.phone().isBlank()) {
            booking.setPhone(request.phone().strip());
        }
        booking.setBookingType(Booking.BookingType.ONLINE);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        booking.setCheckedIn(false);
        booking.setIdempotencyKey(idempotencyKey);
        booking.setCreatedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);
        return BookingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found: " + reference));
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found: " + reference));

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Booking is already cancelled");
        }

        CampSession session = sessionRepository.findByIdForUpdate(booking.getSession().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Service counter not found"));

        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        if (booking.getBookingType() == Booking.BookingType.ONLINE) {
            session.setOnlineReserved(Math.max(0, session.getOnlineReserved() - 1));
            sessionRepository.save(session);
        }

        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse checkIn(String tokenOrReference, HttpServletRequest httpRequest) {
        staffAccessService.requireAnyRole(httpRequest, Staff.Role.VOLUNTEER, Staff.Role.ORGANIZER);

        Booking booking = bookingRepository.findByTokenNumber(tokenOrReference)
                .or(() -> bookingRepository.findByBookingReference(tokenOrReference))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found for: " + tokenOrReference));

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot check in a cancelled booking");
        }

        if (booking.isCheckedIn()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Visitor already checked in at " + booking.getCheckInTime());
        }

        booking.setCheckedIn(true);
        booking.setCheckInTime(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);
        return BookingResponse.from(saved);
    }

    @Transactional
    public BookingResponse registerWalkIn(Long sessionId, WalkInRequest request, HttpServletRequest httpRequest) {
        staffAccessService.requireAnyRole(httpRequest, Staff.Role.VOLUNTEER, Staff.Role.ORGANIZER);

        CampSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Counter not found: " + sessionId));

        if (!session.isOpen()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Service counter is closed");
        }

        if (!session.allowsWalkIn()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "This counter does not accept walk-ins");
        }

        Camp camp = session.getCamp();
        if (camp.isCancelled()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Camp has been cancelled");
        }

        long walkInCount = bookingRepository.countBySession_IdAndBookingType(sessionId, Booking.BookingType.WALK_IN);

        String ref = "WI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String token = "WI-" + sessionId + "-" + String.format("%03d", walkInCount + 1);

        Booking booking = new Booking();
        booking.setBookingReference(ref);
        booking.setTokenNumber(token);
        booking.setSession(session);
        booking.setVisitorName(request.visitorName().strip());
        if (request.phone() != null && !request.phone().isBlank()) {
            booking.setPhone(request.phone().strip());
        }
        booking.setBookingType(Booking.BookingType.WALK_IN);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        booking.setCheckedIn(true);
        booking.setCheckInTime(LocalDateTime.now());
        booking.setCreatedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);
        return BookingResponse.from(saved);
    }

    public AttendanceResponse getAttendance(Long campId, HttpServletRequest httpRequest) {
        staffAccessService.requireAnyRole(httpRequest, Staff.Role.VOLUNTEER, Staff.Role.ORGANIZER, Staff.Role.REVIEWER);

        Camp camp = campRepository.findById(campId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Camp not found: " + campId));

        List<CampSession> sessions = sessionRepository.findByCamp_IdOrderByStartTimeAsc(campId);
        List<AttendanceResponse.CounterAttendance> counters = new ArrayList<>();

        for (CampSession s : sessions) {
            long onlineCheckedIn = bookingRepository.countBySession_IdAndBookingTypeAndCheckedIn(
                    s.getId(), Booking.BookingType.ONLINE, true);
            long walkInCount = bookingRepository.countBySession_IdAndBookingType(
                    s.getId(), Booking.BookingType.WALK_IN);
            long totalCheckedIn = onlineCheckedIn + walkInCount;

            counters.add(new AttendanceResponse.CounterAttendance(
                    s.getId(),
                    s.getServiceName(),
                    s.getBookingMode(),
                    s.getOnlineCapacity(),
                    s.getOnlineReserved(),
                    onlineCheckedIn,
                    walkInCount,
                    totalCheckedIn,
                    s.getStatus()
            ));
        }

        return new AttendanceResponse(camp.getId(), camp.getName(), counters);
    }
}
