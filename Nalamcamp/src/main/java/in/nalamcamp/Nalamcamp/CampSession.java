package in.nalamcamp.Nalamcamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "camp_sessions")
public class CampSession {

    public enum BookingMode {
        ONLINE, WALK_IN, BOTH
    }

    public enum Status {
        OPEN, CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "camp_id", nullable = false)
    private Camp camp;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(name = "items_to_bring", length = 1000)
    private String itemsToBring;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_mode", nullable = false)
    private BookingMode bookingMode;

    @Column(name = "online_capacity", nullable = false)
    private int onlineCapacity;

    @Column(name = "online_reserved", nullable = false)
    private int onlineReserved = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Camp getCamp() {
        return camp;
    }

    public void setCamp(Camp camp) {
        this.camp = camp;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getItemsToBring() {
        return itemsToBring;
    }

    public void setItemsToBring(String itemsToBring) {
        this.itemsToBring = itemsToBring;
    }

    public BookingMode getBookingMode() {
        return bookingMode;
    }

    public void setBookingMode(BookingMode bookingMode) {
        this.bookingMode = bookingMode;
    }

    public int getOnlineCapacity() {
        return onlineCapacity;
    }

    public void setOnlineCapacity(int onlineCapacity) {
        this.onlineCapacity = onlineCapacity;
    }

    public int getOnlineReserved() {
        return onlineReserved;
    }

    public void setOnlineReserved(int onlineReserved) {
        this.onlineReserved = onlineReserved;
    }

    public int getAvailableTokens() {
        return Math.max(0, onlineCapacity - onlineReserved);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean allowsOnlineBooking() {
        return bookingMode == BookingMode.ONLINE || bookingMode == BookingMode.BOTH;
    }

    public boolean allowsWalkIn() {
        return bookingMode == BookingMode.WALK_IN || bookingMode == BookingMode.BOTH;
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }
}
