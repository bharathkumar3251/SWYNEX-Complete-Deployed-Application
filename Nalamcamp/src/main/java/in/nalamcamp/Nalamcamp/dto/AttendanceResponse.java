package in.nalamcamp.Nalamcamp.dto;

import in.nalamcamp.Nalamcamp.CampSession;

import java.util.List;

public record AttendanceResponse(
        Long campId,
        String campName,
        List<CounterAttendance> counters
) {
    public record CounterAttendance(
            Long serviceId,
            String serviceName,
            CampSession.BookingMode bookingMode,
            int onlineCapacity,
            int onlineReserved,
            long onlineCheckedIn,
            long walkInCount,
            long totalCheckedIn,
            CampSession.Status status
    ) {}
}
