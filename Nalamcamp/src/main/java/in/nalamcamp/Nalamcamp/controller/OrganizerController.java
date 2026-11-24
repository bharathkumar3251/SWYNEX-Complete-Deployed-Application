package in.nalamcamp.Nalamcamp.controller;

import in.nalamcamp.Nalamcamp.Organizer;
import in.nalamcamp.Nalamcamp.dto.OrganizerRequest;
import in.nalamcamp.Nalamcamp.service.OrganizerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizers")
public class OrganizerController {

    private final OrganizerService organizerService;

    public OrganizerController(OrganizerService organizerService) {
        this.organizerService = organizerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Organizer createOrganizer(@Valid @RequestBody OrganizerRequest request) {
        return organizerService.createOrganizer(request);
    }

    @GetMapping
    public List<Organizer> getAllOrganizers() {
        return organizerService.getAllOrganizers();
    }

    @GetMapping("/{id}")
    public Organizer getOrganizerById(@PathVariable("id") Long id) {
        return organizerService.getOrganizerById(id);
    }
}