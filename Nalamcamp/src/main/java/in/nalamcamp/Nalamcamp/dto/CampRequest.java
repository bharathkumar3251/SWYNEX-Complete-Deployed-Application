package in.nalamcamp.Nalamcamp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CampRequest(

        @NotBlank(message = "Camp name is required")
        @Size(max = 255, message = "Camp name must be 255 characters or fewer")
        @JsonAlias({"campName", "name"})
        String name,

        @Size(max = 255, message = "Organizer name must be 255 characters or fewer")
        String organizerName,

        @NotNull(message = "Camp date is required")
        @FutureOrPresent(message = "Camp date must be today or later")
        LocalDate campDate,

        @NotBlank(message = "Area is required")
        @Size(max = 255, message = "Area must be 255 characters or fewer")
        String area,

        @NotBlank(message = "Venue is required")
        @Size(max = 255, message = "Venue must be 255 characters or fewer")
        String venue

) {}
