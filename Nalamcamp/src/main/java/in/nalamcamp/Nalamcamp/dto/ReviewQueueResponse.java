package in.nalamcamp.Nalamcamp.dto;

import java.util.List;

public record ReviewQueueResponse(
        List<CampResponse> pendingCamps,
        List<PosterCheckResponse> pendingPosters
) {}
