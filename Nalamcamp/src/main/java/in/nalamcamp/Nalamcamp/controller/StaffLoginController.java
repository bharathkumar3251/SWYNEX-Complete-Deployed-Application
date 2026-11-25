package in.nalamcamp.Nalamcamp.controller;

import in.nalamcamp.Nalamcamp.Staff;
import in.nalamcamp.Nalamcamp.service.StaffAccessService;
import in.nalamcamp.Nalamcamp.service.StaffService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sessions")
public class StaffLoginController {

    private final StaffService staffService;
    private final StaffAccessService staffAccessService;

    public StaffLoginController(StaffService staffService,
                                StaffAccessService staffAccessService) {
        this.staffService = staffService;
        this.staffAccessService = staffAccessService;
    }

    @PostMapping
    public LoginResponse login(@RequestBody LoginRequest request,
                               HttpServletRequest httpRequest) {
        Staff staff = staffService.authenticate(request.email(), request.password());

        HttpSession previousSession = httpRequest.getSession(false);
        if (previousSession != null) {
            previousSession.invalidate();
        }

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("staffId", staff.getId());

        return new LoginResponse(staff.getId(), staff.getRole(), "Login successful");
    }

    public record LoginRequest(String email, String password) {}

    public record LoginResponse(Long staffId, Staff.Role role, String message) {
        public LoginResponse(Long staffId, String message) {
            this(staffId, null, message);
        }
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public CurrentStaffResponse currentStaff(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null || !(session.getAttribute("staffId") instanceof Long)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in");
        }

        Staff staff = staffAccessService.requireStaff(request);
        Long organizerId = staff.getOrganizer() != null ? staff.getOrganizer().getId() : null;
        String organizerName = staff.getOrganizer() != null ? staff.getOrganizer().getName() : null;

        return new CurrentStaffResponse(staff.getId(), staff.getEmail(), staff.getRole(), organizerId, organizerName);
    }

    public record CurrentStaffResponse(
            Long staffId,
            String email,
            Staff.Role role,
            Long organizerId,
            String organizerName
    ) {
        public CurrentStaffResponse(Long staffId) {
            this(staffId, null, null, null, null);
        }
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
