package in.nalamcamp.Nalamcamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "poster_checks")
public class PosterCheck {

    public enum PosterCheckStatus {
        EXTRACTED,
        PENDING_REVIEW,
        MATCHED,
        DISCREPANCIES,
        UNABLE_TO_CONFIRM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poster_check_id", nullable = false, unique = true, length = 64)
    private String posterCheckId;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Lob
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "suggested_organizer")
    private String suggestedOrganizer;

    @Column(name = "suggested_camp_date")
    private LocalDate suggestedCampDate;

    @Column(name = "suggested_venue")
    private String suggestedVenue;

    @Column(name = "suggested_camp_name")
    private String suggestedCampName;

    @Column(name = "corrected_organizer")
    private String correctedOrganizer;

    @Column(name = "corrected_camp_date")
    private LocalDate correctedCampDate;

    @Column(name = "corrected_venue")
    private String correctedVenue;

    @Column(name = "corrected_camp_name")
    private String correctedCampName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PosterCheckStatus status = PosterCheckStatus.EXTRACTED;

    @Column(name = "discrepancies", length = 1000)
    private String discrepancies;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_camp_id")
    private Camp matchedCamp;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_staff_id")
    private Staff reviewedBy;

    @Column(name = "review_notes", length = 1000)
    private String reviewNotes;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPosterCheckId() {
        return posterCheckId;
    }

    public void setPosterCheckId(String posterCheckId) {
        this.posterCheckId = posterCheckId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getSuggestedOrganizer() {
        return suggestedOrganizer;
    }

    public void setSuggestedOrganizer(String suggestedOrganizer) {
        this.suggestedOrganizer = suggestedOrganizer;
    }

    public LocalDate getSuggestedCampDate() {
        return suggestedCampDate;
    }

    public void setSuggestedCampDate(LocalDate suggestedCampDate) {
        this.suggestedCampDate = suggestedCampDate;
    }

    public String getSuggestedVenue() {
        return suggestedVenue;
    }

    public void setSuggestedVenue(String suggestedVenue) {
        this.suggestedVenue = suggestedVenue;
    }

    public String getSuggestedCampName() {
        return suggestedCampName;
    }

    public void setSuggestedCampName(String suggestedCampName) {
        this.suggestedCampName = suggestedCampName;
    }

    public String getCorrectedOrganizer() {
        return correctedOrganizer;
    }

    public void setCorrectedOrganizer(String correctedOrganizer) {
        this.correctedOrganizer = correctedOrganizer;
    }

    public LocalDate getCorrectedCampDate() {
        return correctedCampDate;
    }

    public void setCorrectedCampDate(LocalDate correctedCampDate) {
        this.correctedCampDate = correctedCampDate;
    }

    public String getCorrectedVenue() {
        return correctedVenue;
    }

    public void setCorrectedVenue(String correctedVenue) {
        this.correctedVenue = correctedVenue;
    }

    public String getCorrectedCampName() {
        return correctedCampName;
    }

    public void setCorrectedCampName(String correctedCampName) {
        this.correctedCampName = correctedCampName;
    }

    public PosterCheckStatus getStatus() {
        return status;
    }

    public void setStatus(PosterCheckStatus status) {
        this.status = status;
    }

    public String getDiscrepancies() {
        return discrepancies;
    }

    public void setDiscrepancies(String discrepancies) {
        this.discrepancies = discrepancies;
    }

    public Camp getMatchedCamp() {
        return matchedCamp;
    }

    public void setMatchedCamp(Camp matchedCamp) {
        this.matchedCamp = matchedCamp;
    }

    public Staff getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Staff reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
