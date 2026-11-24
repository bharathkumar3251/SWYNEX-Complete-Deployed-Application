package in.nalamcamp.Nalamcamp.controller;

import in.nalamcamp.Nalamcamp.dto.BookingRequest;
import in.nalamcamp.Nalamcamp.dto.BookingResponse;
import in.nalamcamp.Nalamcamp.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping({"/services/{id}/bookings", "/sessions/{id}/bookings"})
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse reserveToken(@PathVariable("id") Long sessionId,
                                        @Valid @RequestBody BookingRequest request) {
        return bookingService.reserveToken(sessionId, request);
    }

    @GetMapping("/bookings/{reference}")
    public BookingResponse getBooking(@PathVariable("reference") String reference) {
        return bookingService.getBooking(reference);
    }

    @DeleteMapping("/bookings/{reference}")
    public BookingResponse cancelBooking(@PathVariable("reference") String reference) {
        return bookingService.cancelBooking(reference);
    }

    @PostMapping("/bookings/{reference}/cancel")
    public BookingResponse cancelBookingPost(@PathVariable("reference") String reference) {
        return bookingService.cancelBooking(reference);
    }
}
