package in.nalamcamp.Nalamcamp.config;

import in.nalamcamp.Nalamcamp.Organizer;
import in.nalamcamp.Nalamcamp.Staff;
import in.nalamcamp.Nalamcamp.repository.OrganizerRepository;
import in.nalamcamp.Nalamcamp.repository.StaffRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private final StaffRepository staffRepository;
    private final OrganizerRepository organizerRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(StaffRepository staffRepository,
                               OrganizerRepository organizerRepository,
                               PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.organizerRepository = organizerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Ensure default organizer exists
        Organizer organizer = organizerRepository.findById(1L)
                .or(() -> organizerRepository.findAll().stream().findFirst())
                .orElseGet(() -> {
                    Organizer newOrg = new Organizer();
                    newOrg.setName("Nalam Volunteers");
                    newOrg.setOrganizerType(Organizer.OrganizerType.NGO);
                    newOrg.setOfficialContact("contact@example.org");
                    return organizerRepository.save(newOrg);
                });

        // Ensure organizer staff exists
        if (staffRepository.findByEmailIgnoreCase("organizer@nalamcamp.test").isEmpty()) {
            Staff organizerStaff = new Staff();
            organizerStaff.setEmail("organizer@nalamcamp.test");
            organizerStaff.setPasswordHash(passwordEncoder.encode("Organizer@123"));
            organizerStaff.setRole(Staff.Role.ORGANIZER);
            organizerStaff.setOrganizer(organizer);
            staffRepository.save(organizerStaff);
        }

        // Ensure reviewer staff exists
        if (staffRepository.findByEmailIgnoreCase("reviewer@nalamcamp.test").isEmpty()) {
            Staff reviewerStaff = new Staff();
            reviewerStaff.setEmail("reviewer@nalamcamp.test");
            reviewerStaff.setPasswordHash(passwordEncoder.encode("Reviewer@123"));
            reviewerStaff.setRole(Staff.Role.REVIEWER);
            staffRepository.save(reviewerStaff);
        }

        // Ensure volunteer staff exists with known test password
        Staff volunteer = staffRepository.findByEmailIgnoreCase("volunteer@nalamcamp.test")
                .orElseGet(() -> {
                    Staff v = new Staff();
                    v.setEmail("volunteer@nalamcamp.test");
                    v.setRole(Staff.Role.VOLUNTEER);
                    return v;
                });
        volunteer.setPasswordHash(passwordEncoder.encode("Volunteer@123"));
        volunteer.setRole(Staff.Role.VOLUNTEER);
        staffRepository.save(volunteer);
    }
}
