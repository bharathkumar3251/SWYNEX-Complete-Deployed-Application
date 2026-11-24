package in.nalamcamp.Nalamcamp.service;

import in.nalamcamp.Nalamcamp.Camp;
import in.nalamcamp.Nalamcamp.Staff;
import in.nalamcamp.Nalamcamp.repository.StaffRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;

@Service
public class StaffAccessService {

    private final StaffRepository staffRepository;

    public StaffAccessService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    public Staff requireStaff(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null || !(session.getAttribute("staffId") instanceof Long staffId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in");
        }

        return staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Staff account not found"));
    }

    public Staff requireRole(HttpServletRequest request, Staff.Role requiredRole) {
        Staff staff = requireStaff(request);

        if (staff.getRole() != requiredRole) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Your role cannot perform this action");
        }

        return staff;
    }

    public Staff requireAnyRole(HttpServletRequest request, Staff.Role... allowedRoles) {
        Staff staff = requireStaff(request);

        boolean allowed = Arrays.stream(allowedRoles)
                .anyMatch(role -> staff.getRole() == role);

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Your role cannot perform this action");
        }

        return staff;
    }

    public Optional<Staff> getOptionalStaff(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("staffId") instanceof Long staffId)) {
            return Optional.empty();
        }
        return staffRepository.findById(staffId);
    }

    public void checkCampOwnership(Staff staff, Camp camp) {
        if (camp.getOrganizer() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Older unlinked camps cannot be modified or claimed");
        }

        if (staff.getOrganizer() == null ||
                !staff.getOrganizer().getId().equals(camp.getOrganizer().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to manage this camp");
        }
    }
}
