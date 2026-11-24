package in.nalamcamp.Nalamcamp.dto;

public record CampReviewRequest(
        String action, // APPROVE, RETURN_FOR_CORRECTIONS
        String verificationSource,
        String reason
) {}
