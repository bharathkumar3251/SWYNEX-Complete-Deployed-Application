package in.nalamcamp.Nalamcamp.dto;

import in.nalamcamp.Nalamcamp.PosterCheck;

public record PosterReviewRequest(
        Long campId,
        PosterCheck.PosterCheckStatus matchStatus,
        String reviewNotes
) {}
