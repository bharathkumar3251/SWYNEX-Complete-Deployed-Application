package in.nalamcamp.Nalamcamp.repository;

import in.nalamcamp.Nalamcamp.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByEmailIgnoreCase(String email);
}