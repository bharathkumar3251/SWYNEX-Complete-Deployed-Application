package in.nalamcamp.Nalamcamp.config;

import in.nalamcamp.Nalamcamp.Organizer;
import in.nalamcamp.Nalamcamp.Staff;
import in.nalamcamp.Nalamcamp.repository.OrganizerRepository;
import in.nalamcamp.Nalamcamp.repository.StaffRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile({"local-seed", "test"})
public class TestStaffSetup {

    @Bean
    CommandLineRunner createTestStaff(StaffRepository staffRepository,
                                      PasswordEncoder passwordEncoder,
                                      Environment environment) {
        return args -> {
            String email = "volunteer@nalamcamp.test";

            String password = environment.getProperty("NALAMCAMP_TEST_PASSWORD", "Volunteer@123");
            if (password.isBlank()) {
                password = "Volunteer@123";
            }

            if (staffRepository.findByEmailIgnoreCase(email).isPresent()) {
                Staff existing = staffRepository.findByEmailIgnoreCase(email).get();
                existing.setPasswordHash(passwordEncoder.encode(password));
                existing.setRole(Staff.Role.VOLUNTEER);
                staffRepository.save(existing);
                return;
            }

            Staff staff = new Staff();
            staff.setEmail(email);
            staff.setPasswordHash(passwordEncoder.encode(password));
            staff.setRole(Staff.Role.VOLUNTEER);
            staffRepository.save(staff);
        };
    }

    @Bean
    CommandLineRunner createTestOrganizerStaff(
            StaffRepository staffRepository,
            OrganizerRepository organizerRepository,
            PasswordEncoder passwordEncoder,
            Environment environment) {

        return args -> {
            String email = "organizer@nalamcamp.test";

            String password = environment.getProperty(
                    "NALAMCAMP_ORGANIZER_PASSWORD", "Organizer@123");
            if (password.isBlank()) {
                password = "Organizer@123";
            }

            Long organizerId = environment.getProperty(
                    "NALAMCAMP_TEST_ORGANIZER_ID", Long.class, 1L);

            Organizer organizer = organizerRepository.findById(organizerId)
                    .or(() -> organizerRepository.findAll().stream().findFirst())
                    .orElseGet(() -> {
                        Organizer newOrg = new Organizer();
                        newOrg.setName("Nalam Volunteers");
                        newOrg.setOrganizerType(Organizer.OrganizerType.NGO);
                        newOrg.setOfficialContact("contact@example.org");
                        return organizerRepository.save(newOrg);
                    });

            if (staffRepository.findByEmailIgnoreCase(email).isPresent()) {
                Staff existing = staffRepository.findByEmailIgnoreCase(email).get();
                existing.setPasswordHash(passwordEncoder.encode(password));
                existing.setRole(Staff.Role.ORGANIZER);
                existing.setOrganizer(organizer);
                staffRepository.save(existing);
                return;
            }

            Staff staff = new Staff();
            staff.setEmail(email);
            staff.setPasswordHash(passwordEncoder.encode(password));
            staff.setRole(Staff.Role.ORGANIZER);
            staff.setOrganizer(organizer);
            staffRepository.save(staff);
        };
    }

    @Bean
    CommandLineRunner createTestReviewerStaff(
            StaffRepository staffRepository,
            PasswordEncoder passwordEncoder,
            Environment environment) {

        return args -> {
            String email = "reviewer@nalamcamp.test";

            String password = environment.getProperty(
                    "NALAMCAMP_REVIEWER_PASSWORD", "Reviewer@123");
            if (password.isBlank()) {
                password = "Reviewer@123";
            }

            if (staffRepository.findByEmailIgnoreCase(email).isPresent()) {
                Staff existing = staffRepository.findByEmailIgnoreCase(email).get();
                existing.setPasswordHash(passwordEncoder.encode(password));
                existing.setRole(Staff.Role.REVIEWER);
                staffRepository.save(existing);
                return;
            }

            Staff staff = new Staff();
            staff.setEmail(email);
            staff.setPasswordHash(passwordEncoder.encode(password));
            staff.setRole(Staff.Role.REVIEWER);
            staffRepository.save(staff);
        };
    }
}
