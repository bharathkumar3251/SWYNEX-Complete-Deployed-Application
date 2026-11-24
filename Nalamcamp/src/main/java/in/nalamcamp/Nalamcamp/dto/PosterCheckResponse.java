package in.nalamcamp.Nalamcamp.dto;

import in.nalamcamp.Nalamcamp.PosterCheck;

import java.time.LocalDate;

public record PosterCheckResponse(
        String posterCheckId,
        String originalFileName,
        String extractedText,
        String suggestedOrganizer,
        LocalDate suggestedCampDate,
        String suggestedVenue,
        String suggestedCampName,
        PosterCheck.PosterCheckStatus status,
        String discrepancies,
        Long matchedCampId,
        String verificationSource
) {
    public static PosterCheckResponse from(PosterCheck posterCheck) {
        String verificationSource = posterCheck.getMatchedCamp() != null ?
                posterCheck.getMatchedCamp().getVerificationSource() : null;
        Long matchedCampId = posterCheck.getMatchedCamp() != null ?
                posterCheck.getMatchedCamp().getId() : null;

        return new PosterCheckResponse(
                posterCheck.getPosterCheckId(),
                posterCheck.getOriginalFileName(),
                posterCheck.getExtractedText(),
                posterCheck.getSuggestedOrganizer(),
                posterCheck.getSuggestedCampDate(),
                posterCheck.getSuggestedVenue(),
                posterCheck.getSuggestedCampName(),
                posterCheck.getStatus(),
                posterCheck.getDiscrepancies(),
                matchedCampId,
                verificationSource
        );
    }
}
