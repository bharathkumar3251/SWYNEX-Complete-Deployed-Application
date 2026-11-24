package in.nalamcamp.Nalamcamp.dto;

import in.nalamcamp.Nalamcamp.Booking;
import in.nalamcamp.Nalamcamp.CampSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(
        String bookingReference,
        String tokenNumber,
        Long serviceId,
        String serviceName,
        String reportingWindow,
        String venue,
        String campName,
        LocalDate campDate,
        String instructions,
        BigDecimal fee,
        Booking.BookingStatus status,
        Booking.BookingType bookingType,
        boolean checkedIn,
        LocalDateTime checkInTime
) {
    public static BookingResponse from(Booking booking) {
        CampSession session = booking.getSession();
        String reportingWindow = session.getStartTime() + " - " + session.getEndTime();
        String venue = session.getCamp() != null ? session.getCamp().getVenue() : "";
        String campName = session.getCamp() != null ? session.getCamp().getName() : "";
        LocalDate campDate = session.getCamp() != null ? session.getCamp().getCampDate() : null;

        return new BookingResponse(
                booking.getBookingReference(),
                booking.getTokenNumber(),
                session.getId(),
                session.getServiceName(),
                reportingWindow,
                venue,
                campName,
                campDate,
                session.getItemsToBring(),
                session.getFee(),
                booking.getBookingStatus(),
                booking.getBookingType(),
                booking.isCheckedIn(),
                booking.getCheckInTime()
        );
    }
}
