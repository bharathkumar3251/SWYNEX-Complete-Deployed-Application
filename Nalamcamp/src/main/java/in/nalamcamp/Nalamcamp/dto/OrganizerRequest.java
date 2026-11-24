package in.nalamcamp.Nalamcamp.dto;

import in.nalamcamp.Nalamcamp.Organizer.OrganizerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrganizerRequest(

        @NotBlank(message = "Organizer name is required")
        @Size(max = 255, message = "Organizer name must be 255 characters or fewer")
        String name,

        @NotNull(message = "Organizer type is required")
        OrganizerType organizerType,

        @NotBlank(message = "Official contact is required")
        @Size(max = 255, message = "Official contact must be 255 characters or fewer")
        String officialContact

) {}