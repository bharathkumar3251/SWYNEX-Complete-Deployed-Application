package in.nalamcamp.Nalamcamp.dto;

import java.time.LocalDate;

public record PosterMatchRequest(
        String organizerName,
        LocalDate campDate,
        String venue,
        String campName
) {}
