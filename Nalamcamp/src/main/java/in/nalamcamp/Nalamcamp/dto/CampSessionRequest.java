package in.nalamcamp.Nalamcamp.dto;

import in.nalamcamp.Nalamcamp.CampSession.BookingMode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalTime;

public record CampSessionRequest(

        @NotBlank(message = "Service name is required")
        @Size(max = 255, message = "Service name must be 255 characters or fewer")
        String serviceName,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        @PositiveOrZero(message = "Fee cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Use up to 8 digits and 2 decimal places")
        BigDecimal fee,

        @Size(max = 1000, message = "Items to bring must be 1000 characters or fewer")
        String itemsToBring,

        @NotNull(message = "Booking mode is required")
        BookingMode bookingMode,

        @NotNull(message = "Online capacity is required")
        @PositiveOrZero(message = "Online capacity cannot be negative")
        Integer onlineCapacity

) {
    @AssertTrue(message = "End time must be after start time")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }

    @AssertTrue(message = "A walk-in-only counter must have zero online capacity")
    public boolean isWalkInCapacityValid() {
        if (bookingMode != BookingMode.WALK_IN || onlineCapacity == null) {
            return true;
        }
        return onlineCapacity == 0;
    }
}