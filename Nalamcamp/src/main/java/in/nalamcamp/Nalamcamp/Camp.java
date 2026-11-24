package in.nalamcamp.Nalamcamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "camps")
public class Camp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "organizer_name", nullable = false)
    private String organizerName;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organizer_id")
    private Organizer organizer;

    @Column(name = "camp_date", nullable = false)
    private LocalDate campDate;

    @Column(nullable = false)
    private String area;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private String status = "DRAFT";

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_staff_id")
    private Staff reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "verification_source", length = 500)
    private String verificationSource;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @JsonIgnore
    @OneToMany(mappedBy = "camp", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CampSession> sessions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public Organizer getOrganizer() {
        return organizer;
    }

    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }

    public Organizer.OrganizerType getOrganizerType() {
        return organizer != null ? organizer.getOrganizerType() : null;
    }

    public LocalDate getCampDate() {
        return campDate;
    }

    public void setCampDate(LocalDate campDate) {
        this.campDate = campDate;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Staff getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Staff reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getVerificationSource() {
        return verificationSource;
    }

    public void setVerificationSource(String verificationSource) {
        this.verificationSource = verificationSource;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public List<CampSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<CampSession> sessions) {
        this.sessions = sessions;
    }

    public boolean isDraft() {
        return "DRAFT".equalsIgnoreCase(status);
    }

    public boolean isPendingReview() {
        return "PENDING_REVIEW".equalsIgnoreCase(status);
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }
}
