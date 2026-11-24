package in.nalamcamp.Nalamcamp;

import in.nalamcamp.Nalamcamp.dto.*;
import in.nalamcamp.Nalamcamp.repository.BookingRepository;
import in.nalamcamp.Nalamcamp.repository.CampRepository;
import in.nalamcamp.Nalamcamp.repository.CampSessionRepository;
import in.nalamcamp.Nalamcamp.repository.StaffRepository;
import in.nalamcamp.Nalamcamp.service.BookingService;
import in.nalamcamp.Nalamcamp.service.CampService;
import in.nalamcamp.Nalamcamp.service.CampSessionService;
import in.nalamcamp.Nalamcamp.service.StaffAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class NalamcampIntegrationTests {

    @Autowired
    private CampService campService;

    @Autowired
    private CampSessionService sessionService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CampRepository campRepository;

    @Autowired
    private CampSessionRepository sessionRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StaffAccessService staffAccessService;

    @Autowired
    private in.nalamcamp.Nalamcamp.service.PosterCheckService posterCheckService;

    @Autowired
    private in.nalamcamp.Nalamcamp.repository.PosterCheckRepository posterCheckRepository;

    private MockHttpServletRequest organizerRequest;
    private MockHttpServletRequest reviewerRequest;
    private MockHttpServletRequest volunteerRequest;

    @BeforeEach
    void setupRequests() {
        Staff organizer = staffRepository.findByEmailIgnoreCase("organizer@nalamcamp.test").orElseThrow();
        Staff reviewer = staffRepository.findByEmailIgnoreCase("reviewer@nalamcamp.test").orElseThrow();
        Staff volunteer = staffRepository.findByEmailIgnoreCase("volunteer@nalamcamp.test").orElseThrow();

        MockHttpSession orgSession = new MockHttpSession();
        orgSession.setAttribute("staffId", organizer.getId());
        organizerRequest = new MockHttpServletRequest();
        organizerRequest.setSession(orgSession);

        MockHttpSession revSession = new MockHttpSession();
        revSession.setAttribute("staffId", reviewer.getId());
        reviewerRequest = new MockHttpServletRequest();
        reviewerRequest.setSession(revSession);

        MockHttpSession volSession = new MockHttpSession();
        volSession.setAttribute("staffId", volunteer.getId());
        volunteerRequest = new MockHttpServletRequest();
        volunteerRequest.setSession(volSession);
    }

    @Test
    @DisplayName("Camp Lifecycle: Create Draft -> Add Service -> Submit -> Reviewer Approve")
    void testCampLifecycle() {
        CampRequest campReq = new CampRequest(
                "Health Screening Drive",
                null,
                LocalDate.now().plusDays(10),
                "Coimbatore South",
                "Community Center"
        );
        Camp camp = campService.createCamp(campReq, organizerRequest);
        assertNotNull(camp.getId());
        assertEquals("DRAFT", camp.getStatus());
        assertNotNull(camp.getOrganizer());

        CampSessionRequest sessionReq = new CampSessionRequest(
                "Dental Screening",
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                BigDecimal.ZERO,
                "Bring previous medical cards if any",
                CampSession.BookingMode.BOTH,
                5
        );
        CampSession session = sessionService.createSession(camp.getId(), sessionReq, organizerRequest);
        assertNotNull(session.getId());

        CampResponse submitted = campService.submitCamp(camp.getId(), organizerRequest);
        assertEquals("PENDING_REVIEW", submitted.status());

        CampReviewRequest approveReq = new CampReviewRequest(
                "APPROVE",
                "Official bulletin confirmation #8821",
                null
        );
        CampResponse approved = campService.reviewCamp(camp.getId(), approveReq, reviewerRequest);
        assertEquals("APPROVED", approved.status());
        assertEquals("Official bulletin confirmation #8821", approved.verificationSource());
    }

    @Test
    @DisplayName("Reviewer Return for Correction: Status returns to DRAFT with reason")
    void testReviewerReturnForCorrection() {
        CampRequest campReq = new CampRequest(
                "Vision Check Camp",
                null,
                LocalDate.now().plusDays(15),
                "Pollachi North",
                "Old School Hall"
        );
        Camp camp = campService.createCamp(campReq, organizerRequest);

        CampSessionRequest sessionReq = new CampSessionRequest(
                "Eye Consultation",
                LocalTime.of(10, 0),
                LocalTime.of(13, 0),
                BigDecimal.ZERO,
                "Glasses",
                CampSession.BookingMode.BOTH,
                10
        );
        sessionService.createSession(camp.getId(), sessionReq, organizerRequest);
        campService.submitCamp(camp.getId(), organizerRequest);

        CampReviewRequest rejectReq = new CampReviewRequest(
                "RETURN_FOR_CORRECTIONS",
                null,
                "Please update specific venue room number and wheelchair access details."
        );
        CampResponse reviewed = campService.reviewCamp(camp.getId(), rejectReq, reviewerRequest);
        assertEquals("DRAFT", reviewed.status());
        assertEquals("Please update specific venue room number and wheelchair access details.", reviewed.rejectionReason());
    }

    @Test
    @DisplayName("Public Search hides draft camps and exposes approved camps")
    void testPublicSearchVisibility() {
        CampRequest campReq = new CampRequest(
                "Public Visibility Camp",
                null,
                LocalDate.now().plusDays(5),
                "Udumalpet East",
                "Sub-Center"
        );
        Camp camp = campService.createCamp(campReq, organizerRequest);

        // Before approval, draft must not appear in public search
        List<CampResponse> searchDraft = campService.getPublicCamps("Udumalpet East", null, null);
        assertTrue(searchDraft.stream().noneMatch(c -> c.id().equals(camp.getId())));

        // Direct lookup by anonymous user must return 404
        assertThrows(ResponseStatusException.class, () ->
                campService.getCampResponseById(camp.getId(), new MockHttpServletRequest()));

        // Add counter, submit, approve
        CampSessionRequest sReq = new CampSessionRequest(
                "Pediatric Check",
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                BigDecimal.ZERO,
                "None",
                CampSession.BookingMode.BOTH,
                5
        );
        sessionService.createSession(camp.getId(), sReq, organizerRequest);
        campService.submitCamp(camp.getId(), organizerRequest);
        campService.reviewCamp(camp.getId(), new CampReviewRequest("APPROVE", "Dept Ref #102", null), reviewerRequest);

        // Now it must appear in public search
        List<CampResponse> searchApproved = campService.getPublicCamps("Udumalpet", null, "Pediatric");
        assertTrue(searchApproved.stream().anyMatch(c -> c.id().equals(camp.getId())));
    }

    @Test
    @DisplayName("Booking Concurrency Test: Last slot reserved by exactly one thread")
    void testConcurrentLastTokenBooking() throws Exception {
        CampRequest campReq = new CampRequest(
                "Concurrency Test Camp",
                null,
                LocalDate.now().plusDays(3),
                "Sulur",
                "Panchayat Bhavan"
        );
        Camp camp = campService.createCamp(campReq, organizerRequest);

        CampSessionRequest sReq = new CampSessionRequest(
                "Last Slot Consultation",
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                BigDecimal.ZERO,
                "ID Card",
                CampSession.BookingMode.ONLINE,
                1 // Only 1 token available!
        );
        CampSession session = sessionService.createSession(camp.getId(), sReq, organizerRequest);
        campService.submitCamp(camp.getId(), organizerRequest);
        campService.reviewCamp(camp.getId(), new CampReviewRequest("APPROVE", "Official Memo 5", null), reviewerRequest);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                barrier.await();
                try {
                    BookingRequest bReq = new BookingRequest("Visitor " + index, "987654321" + index, null);
                    BookingResponse resp = bookingService.reserveToken(session.getId(), bReq);
                    return resp != null && resp.status() == Booking.BookingStatus.CONFIRMED;
                } catch (ResponseStatusException e) {
                    if (e.getStatusCode().value() == 409) {
                        return false; // Expected conflict for the losing thread
                    }
                    throw e;
                }
            }));
        }

        int successCount = 0;
        int failureCount = 0;
        for (Future<Boolean> f : futures) {
            if (f.get(15, TimeUnit.SECONDS)) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        executor.shutdown();

        assertEquals(1, successCount, "Exactly one thread must succeed in booking the last slot");
        assertEquals(1, failureCount, "The other thread must be rejected with 409 Conflict");

        CampSession updatedSession = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(1, updatedSession.getOnlineReserved());
        assertEquals(0, updatedSession.getAvailableTokens());
    }

    @Test
    @DisplayName("Idempotency: Repeated request with same key returns original token")
    void testIdempotentBookingRetry() {
        CampRequest campReq = new CampRequest(
                "Idempotency Camp",
                null,
                LocalDate.now().plusDays(4),
                "Annur",
                "Primary School"
        );
        Camp camp = campService.createCamp(campReq, organizerRequest);
        CampSession session = sessionService.createSession(camp.getId(), new CampSessionRequest(
                "Blood Pressure Check",
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                BigDecimal.ZERO,
                "None",
                CampSession.BookingMode.ONLINE,
                5
        ), organizerRequest);
        campService.submitCamp(camp.getId(), organizerRequest);
        campService.reviewCamp(camp.getId(), new CampReviewRequest("APPROVE", "Bulletin A", null), reviewerRequest);

        String key = "IDEMP-KEY-" + java.util.UUID.randomUUID();
        BookingRequest bReq = new BookingRequest("Alice Walker", "9845123456", key);

        BookingResponse firstResp = bookingService.reserveToken(session.getId(), bReq);
        assertNotNull(firstResp);

        // Same key, same details -> safe retry, returns original token
        BookingResponse secondResp = bookingService.reserveToken(session.getId(), bReq);
        assertEquals(firstResp.bookingReference(), secondResp.bookingReference());
        assertEquals(firstResp.tokenNumber(), secondResp.tokenNumber());

        // Same key, different visitor -> must be rejected with 409
        BookingRequest conflictReq = new BookingRequest("Bob Martin", "9845000000", key);
        assertThrows(ResponseStatusException.class, () ->
                bookingService.reserveToken(session.getId(), conflictReq));
    }

    @Test
    @DisplayName("Check-In: Duplicate check-in is prevented")
    void testDuplicateCheckInPrevention() {
        CampRequest campReq = new CampRequest(
                "Check-in Verification Camp",
                null,
                LocalDate.now().plusDays(6),
                "Ooty Road",
                "Town Hall"
        );
        Camp camp = campService.createCamp(campReq, organizerRequest);
        CampSession session = sessionService.createSession(camp.getId(), new CampSessionRequest(
                "General Medicine",
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                BigDecimal.ZERO,
                "None",
                CampSession.BookingMode.ONLINE,
                5
        ), organizerRequest);
        campService.submitCamp(camp.getId(), organizerRequest);
        campService.reviewCamp(camp.getId(), new CampReviewRequest("APPROVE", "Notice #33", null), reviewerRequest);

        BookingResponse booking = bookingService.reserveToken(
                session.getId(), new BookingRequest("David Miller", "9871122334", null));

        // First check-in succeeds
        BookingResponse checkedIn = bookingService.checkIn(booking.tokenNumber(), volunteerRequest);
        assertTrue(checkedIn.checkedIn());
        assertNotNull(checkedIn.checkInTime());

        // Second check-in must fail with 409 Conflict
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                bookingService.checkIn(booking.tokenNumber(), volunteerRequest));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Cancellation: Releases capacity exactly once")
    void testBookingCancellationReleasesCapacity() {
        CampRequest campReq = new CampRequest(
                "Capacity Release Camp",
                null,
                LocalDate.now().plusDays(7),
                "Perur",
                "Temple Grounds"
        );
        Camp camp = campService.createCamp(campReq, organizerRequest);
        CampSession session = sessionService.createSession(camp.getId(), new CampSessionRequest(
                "Ayush Consultation",
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                BigDecimal.ZERO,
                "None",
                CampSession.BookingMode.ONLINE,
                2
        ), organizerRequest);
        campService.submitCamp(camp.getId(), organizerRequest);
        campService.reviewCamp(camp.getId(), new CampReviewRequest("APPROVE", "Ayush Board #9", null), reviewerRequest);

        BookingResponse booking = bookingService.reserveToken(
                session.getId(), new BookingRequest("Grace Hopper", "9812345678", null));

        CampSession s1 = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(1, s1.getOnlineReserved());

        // Cancel booking
        BookingResponse cancelled = bookingService.cancelBooking(booking.bookingReference());
        assertEquals(Booking.BookingStatus.CANCELLED, cancelled.status());

        CampSession s2 = sessionRepository.findById(session.getId()).orElseThrow();
        assertEquals(0, s2.getOnlineReserved());

        // Cancelling again must fail
        assertThrows(ResponseStatusException.class, () ->
                bookingService.cancelBooking(booking.bookingReference()));
    }

    @Test
    @DisplayName("Poster Check: Match against approved camp returns status MATCHED and verification source")
    void testPosterCheckExactMatch() {
        long uniqueKey = System.currentTimeMillis();
        LocalDate campDate = LocalDate.now().plusDays(250 + (int) (uniqueKey % 100));
        String uniqueCampName = "Poster Verified Health Camp " + uniqueKey;
        String uniqueVenue = "Town Hall " + uniqueKey;

        CampRequest campReq = new CampRequest(
                uniqueCampName,
                null,
                campDate,
                "Kinathukadavu",
                uniqueVenue
        );
        Camp camp = campService.createCamp(campReq, organizerRequest);
        sessionService.createSession(camp.getId(), new CampSessionRequest(
                "Dental Care", LocalTime.of(9, 0), LocalTime.of(12, 0), BigDecimal.ZERO, "None",
                CampSession.BookingMode.BOTH, 10), organizerRequest);
        campService.submitCamp(camp.getId(), organizerRequest);
        campService.reviewCamp(camp.getId(), new CampReviewRequest("APPROVE", "Govt Health Memo #2026-88", null), reviewerRequest);

        String pcId = "PC-TEST-MATCH-" + java.util.UUID.randomUUID();
        PosterCheck pc = new PosterCheck();
        pc.setPosterCheckId(pcId);
        pc.setOriginalFileName("poster.png");
        pc.setStatus(PosterCheck.PosterCheckStatus.EXTRACTED);
        pc = posterCheckRepository.save(pc);

        // Corrected match request with matching unique date, venue, and camp title
        PosterMatchRequest matchReq = new PosterMatchRequest(
                "Nalam Volunteers",
                campDate,
                uniqueVenue,
                uniqueCampName
        );

        PosterCheckResponse matchResp = posterCheckService.matchPoster(pcId, matchReq);
        assertEquals(PosterCheck.PosterCheckStatus.MATCHED, matchResp.status());
        assertEquals("Govt Health Memo #2026-88", matchResp.verificationSource());
        assertEquals(camp.getId(), matchResp.matchedCampId());
    }

    @Test
    @DisplayName("Poster Check: Discrepancy detected when venue or organizer differs")
    void testPosterCheckDiscrepancy() {
        long uniqueKey = System.currentTimeMillis() + 1000;
        LocalDate campDate = LocalDate.now().plusDays(360 + (int) (uniqueKey % 100));
        String uniqueCampName = "Discrepancy Check Camp " + uniqueKey;
        String approvedVenue = "Central Hospital Grounds " + uniqueKey;
        String differentVenue = "Private Hotel Ballroom " + uniqueKey;

        CampRequest campReq = new CampRequest(
                uniqueCampName,
                null,
                campDate,
                "Pollachi",
                approvedVenue
        );
        Camp camp = campService.createCamp(campReq, organizerRequest);
        sessionService.createSession(camp.getId(), new CampSessionRequest(
                "General", LocalTime.of(9, 0), LocalTime.of(12, 0), BigDecimal.ZERO, "None",
                CampSession.BookingMode.BOTH, 10), organizerRequest);
        campService.submitCamp(camp.getId(), organizerRequest);
        campService.reviewCamp(camp.getId(), new CampReviewRequest("APPROVE", "Official Source A", null), reviewerRequest);

        String pcId = "PC-TEST-DISCREP-" + java.util.UUID.randomUUID();
        PosterCheck pc = new PosterCheck();
        pc.setPosterCheckId(pcId);
        pc.setOriginalFileName("poster2.jpg");
        pc.setStatus(PosterCheck.PosterCheckStatus.EXTRACTED);
        posterCheckRepository.save(pc);

        // Date and name match, but venue is different
        PosterMatchRequest diffVenueReq = new PosterMatchRequest(
                "Nalam Volunteers",
                campDate,
                differentVenue,
                uniqueCampName
        );

        PosterCheckResponse resp = posterCheckService.matchPoster(pcId, diffVenueReq);
        assertEquals(PosterCheck.PosterCheckStatus.DISCREPANCIES, resp.status());
        assertNotNull(resp.discrepancies());
        assertTrue(resp.discrepancies().contains("Venue discrepancy"));
    }

    @Test
    @DisplayName("Poster Check: Equal matching between two approved camps returns UNABLE_TO_CONFIRM")
    void testPosterCheckEqualMatchAmbiguityReturnsUnableToConfirm() {
        long uniqueKey = System.currentTimeMillis() + 2000;
        LocalDate campDate = LocalDate.now().plusDays(400 + (int) (uniqueKey % 50));
        String sharedCampName = "Shared Title Camp " + uniqueKey;
        String sharedVenue = "Panchayat Hall " + uniqueKey;

        // Create Camp 1
        Camp camp1 = campService.createCamp(new CampRequest(
                sharedCampName, null, campDate, "Area 1", sharedVenue), organizerRequest);
        sessionService.createSession(camp1.getId(), new CampSessionRequest(
                "General", LocalTime.of(9, 0), LocalTime.of(12, 0), BigDecimal.ZERO, "None",
                CampSession.BookingMode.BOTH, 10), organizerRequest);
        campService.submitCamp(camp1.getId(), organizerRequest);
        campService.reviewCamp(camp1.getId(), new CampReviewRequest("APPROVE", "Source 1", null), reviewerRequest);

        // Create Camp 2 with identical matching details
        Camp camp2 = campService.createCamp(new CampRequest(
                sharedCampName, null, campDate, "Area 2", sharedVenue), organizerRequest);
        sessionService.createSession(camp2.getId(), new CampSessionRequest(
                "General", LocalTime.of(9, 0), LocalTime.of(12, 0), BigDecimal.ZERO, "None",
                CampSession.BookingMode.BOTH, 10), organizerRequest);
        campService.submitCamp(camp2.getId(), organizerRequest);
        campService.reviewCamp(camp2.getId(), new CampReviewRequest("APPROVE", "Source 2", null), reviewerRequest);

        String pcId = "PC-TEST-AMBIGUOUS-" + java.util.UUID.randomUUID();
        PosterCheck pc = new PosterCheck();
        pc.setPosterCheckId(pcId);
        pc.setOriginalFileName("poster_ambiguous.jpg");
        pc.setStatus(PosterCheck.PosterCheckStatus.EXTRACTED);
        posterCheckRepository.save(pc);

        PosterMatchRequest ambigReq = new PosterMatchRequest(
                "Nalam Volunteers",
                campDate,
                sharedVenue,
                sharedCampName
        );

        PosterCheckResponse resp = posterCheckService.matchPoster(pcId, ambigReq);
        assertEquals(PosterCheck.PosterCheckStatus.UNABLE_TO_CONFIRM, resp.status());
        assertNull(resp.matchedCampId());
        assertNotNull(resp.discrepancies());
        assertTrue(resp.discrepancies().contains("Multiple approved camps match"));
    }

    @Test
    @DisplayName("Security: 401 for unauthenticated, 403 for incorrect role and ownership")
    void testSecurityAndOwnershipEnforcement() {
        MockHttpServletRequest anonymousReq = new MockHttpServletRequest();

        // 401 when anonymous user tries to create camp
        CampRequest campReq = new CampRequest(
                "Unauthorized Camp", null, LocalDate.now().plusDays(30), "Area", "Venue");
        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class, () ->
                campService.createCamp(campReq, anonymousReq));
        assertEquals(401, ex1.getStatusCode().value());

        // 403 when Volunteer tries to create camp (only ORGANIZER allowed)
        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () ->
                campService.createCamp(campReq, volunteerRequest));
        assertEquals(403, ex2.getStatusCode().value());

        // 403 when Volunteer tries to review camp (only REVIEWER allowed)
        Camp camp = campService.createCamp(campReq, organizerRequest);
        ResponseStatusException ex3 = assertThrows(ResponseStatusException.class, () ->
                campService.reviewCamp(camp.getId(), new CampReviewRequest("APPROVE", "Src", null), volunteerRequest));
        assertEquals(403, ex3.getStatusCode().value());

        // 403 when trying to update older unlinked camp (where organizer_id is null)
        Camp olderCamp = new Camp();
        olderCamp.setName("Legacy Camp");
        olderCamp.setOrganizerName("Old Org");
        olderCamp.setCampDate(LocalDate.now().plusDays(40));
        olderCamp.setArea("Legacy Area");
        olderCamp.setVenue("Legacy Venue");
        olderCamp.setStatus("DRAFT");
        olderCamp = campRepository.save(olderCamp);

        final Long olderId = olderCamp.getId();
        ResponseStatusException ex4 = assertThrows(ResponseStatusException.class, () ->
                campService.updateCamp(olderId, campReq, organizerRequest));
        assertEquals(403, ex4.getStatusCode().value());
        assertTrue(ex4.getReason().contains("Older unlinked camps"));
    }
}
