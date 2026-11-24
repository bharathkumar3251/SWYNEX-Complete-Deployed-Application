package in.nalamcamp.Nalamcamp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WalkInRequest(

        @NotBlank(message = "Visitor name is required")
        @Size(max = 255, message = "Visitor name must be 255 characters or fewer")
        String visitorName,

        @Size(max = 32, message = "Phone must be 32 characters or fewer")
        String phone

) {}
