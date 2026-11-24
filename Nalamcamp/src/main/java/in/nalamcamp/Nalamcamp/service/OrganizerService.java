package in.nalamcamp.Nalamcamp.service;

import in.nalamcamp.Nalamcamp.Organizer;
import in.nalamcamp.Nalamcamp.dto.OrganizerRequest;
import in.nalamcamp.Nalamcamp.repository.OrganizerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OrganizerService {

    private final OrganizerRepository organizerRepository;

    public OrganizerService(OrganizerRepository organizerRepository) {
        this.organizerRepository = organizerRepository;
    }

    public Organizer createOrganizer(OrganizerRequest request) {
        Organizer organizer = new Organizer();
        organizer.setName(request.name().strip());
        organizer.setOrganizerType(request.organizerType());
        organizer.setOfficialContact(request.officialContact().strip());

        return organizerRepository.save(organizer);
    }

    public List<Organizer> getAllOrganizers() {
        return organizerRepository.findAll();
    }

    public Organizer getOrganizerById(Long id) {
        return organizerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Organizer not found: " + id));
    }
}