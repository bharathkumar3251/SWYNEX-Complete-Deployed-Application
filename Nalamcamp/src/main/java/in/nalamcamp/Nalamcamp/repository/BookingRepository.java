package in.nalamcamp.Nalamcamp.repository;

import in.nalamcamp.Nalamcamp.Booking;
import in.nalamcamp.Nalamcamp.Booking.BookingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Optional<Booking> findByTokenNumber(String tokenNumber);

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    List<Booking> findBySession_Id(Long sessionId);

    List<Booking> findBySession_Camp_Id(Long campId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.session.id = :sessionId AND b.bookingType = :bookingType AND b.checkedIn = :checkedIn")
    long countBySession_IdAndBookingTypeAndCheckedIn(
            @Param("sessionId") Long sessionId,
            @Param("bookingType") BookingType bookingType,
            @Param("checkedIn") boolean checkedIn);

    long countBySession_IdAndBookingType(Long sessionId, BookingType bookingType);
}
