package in.nalamcamp.Nalamcamp.repository;

import in.nalamcamp.Nalamcamp.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizerRepository extends JpaRepository<Organizer, Long> {

    Optional<Organizer> findByNameIgnoreCase(String name);
}
