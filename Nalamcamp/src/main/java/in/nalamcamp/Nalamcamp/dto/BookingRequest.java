package in.nalamcamp.Nalamcamp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookingRequest(

        @NotBlank(message = "Visitor name is required")
        @Size(max = 255, message = "Visitor name must be 255 characters or fewer")
        @JsonAlias({"residentName", "name", "visitorName"})
        String visitorName,

        @Size(max = 32, message = "Phone must be 32 characters or fewer")
        @JsonAlias({"residentPhone", "visitorPhone", "phone"})
        String phone,

        @Size(max = 128, message = "Idempotency key must be 128 characters or fewer")
        String idempotencyKey

) {}
