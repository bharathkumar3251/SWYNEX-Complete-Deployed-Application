package in.nalamcamp.Nalamcamp.controller;

import in.nalamcamp.Nalamcamp.dto.AttendanceResponse;
import in.nalamcamp.Nalamcamp.dto.BookingResponse;
import in.nalamcamp.Nalamcamp.dto.WalkInRequest;
import in.nalamcamp.Nalamcamp.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final BookingService bookingService;

    public AttendanceController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/camps/{id}/attendance")
    public AttendanceResponse getAttendance(@PathVariable("id") Long campId,
                                            HttpServletRequest httpRequest) {
        return bookingService.getAttendance(campId, httpRequest);
    }

    @PostMapping("/services/{id}/walk-ins")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse registerWalkIn(@PathVariable("id") Long sessionId,
                                          @Valid @RequestBody WalkInRequest request,
                                          HttpServletRequest httpRequest) {
        return bookingService.registerWalkIn(sessionId, request, httpRequest);
    }

    @PatchMapping("/bookings/{token}/check-in")
    public BookingResponse checkInPatch(@PathVariable("token") String token,
                                        HttpServletRequest httpRequest) {
        return bookingService.checkIn(token, httpRequest);
    }

    @PostMapping("/bookings/{token}/check-in")
    public BookingResponse checkInPost(@PathVariable("token") String token,
                                       HttpServletRequest httpRequest) {
        return bookingService.checkIn(token, httpRequest);
    }
}
