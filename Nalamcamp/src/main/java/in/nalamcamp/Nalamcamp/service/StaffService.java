package in.nalamcamp.Nalamcamp.service;

import in.nalamcamp.Nalamcamp.Staff;
import in.nalamcamp.Nalamcamp.repository.StaffRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffService {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffService(StaffRepository staffRepository,
                        PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Staff authenticate(String email, String password) {
        if (email == null || email.isBlank() ||
                password == null || password.isEmpty()) {
            throw invalidCredentials();
        }

        Staff staff = staffRepository.findByEmailIgnoreCase(email.strip())
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(password, staff.getPasswordHash())) {
            throw invalidCredentials();
        }

        return staff;
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid email or password"
        );
    }
}