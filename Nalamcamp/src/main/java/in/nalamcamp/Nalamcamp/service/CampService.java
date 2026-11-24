package in.nalamcamp.Nalamcamp.service;

import in.nalamcamp.Nalamcamp.Camp;
import in.nalamcamp.Nalamcamp.CampSession;
import in.nalamcamp.Nalamcamp.Organizer;
import in.nalamcamp.Nalamcamp.Staff;
import in.nalamcamp.Nalamcamp.dto.CampRequest;
import in.nalamcamp.Nalamcamp.dto.CampResponse;
import in.nalamcamp.Nalamcamp.dto.CampReviewRequest;
import in.nalamcamp.Nalamcamp.repository.CampRepository;
import in.nalamcamp.Nalamcamp.repository.CampSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CampService {

    private final CampRepository campRepository;
    private final CampSessionRepository sessionRepository;
    private final StaffAccessService staffAccessService;

    public CampService(CampRepository campRepository,
                       CampSessionRepository sessionRepository,
                       StaffAccessService staffAccessService) {
        this.campRepository = campRepository;
        this.sessionRepository = sessionRepository;
        this.staffAccessService = staffAccessService;
    }

    @Transactional
    public Camp createCamp(CampRequest request, HttpServletRequest httpRequest) {
        Staff staff = staffAccessService.requireRole(httpRequest, Staff.Role.ORGANIZER);
        return createCamp(request, staff);
    }

    @Transactional
    public Camp createCamp(CampRequest request, Staff staff) {
        Organizer organizer = staff.getOrganizer();
        if (organizer == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your staff account is not linked to an organizer");
        }

        Camp camp = new Camp();
        camp.setOrganizer(organizer);
        camp.setOrganizerName(organizer.getName());
        camp.setStatus("DRAFT");
        copyDetails(camp, request);

        return campRepository.save(camp);
    }

    public List<Camp> getAllCamps() {
        return campRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CampResponse> getPublicCamps(String area, LocalDate date, String service) {
        String areaParam = (area != null && !area.isBlank()) ? area.strip() : null;
        String serviceParam = (service != null && !service.isBlank()) ? service.strip() : null;

        List<Camp> camps = campRepository.findPublicCamps(areaParam, date, serviceParam);
        return camps.stream()
                .map(this::toResponse)
                .toList();
    }

    public Camp getCampById(Long id) {
        return campRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Camp not found: " + id));
    }

    @Transactional(readOnly = true)
    public CampResponse getCampResponseById(Long id, HttpServletRequest httpRequest) {
        Camp camp = getCampById(id);

        if (camp.isDraft() || camp.isPendingReview()) {
            Optional<Staff> staffOpt = staffAccessService.getOptionalStaff(httpRequest);
            boolean allowed = false;
            if (staffOpt.isPresent()) {
                Staff staff = staffOpt.get();
                if (staff.getRole() == Staff.Role.REVIEWER) {
                    allowed = true;
                } else if (staff.getRole() == Staff.Role.ORGANIZER &&
                        camp.getOrganizer() != null &&
                        staff.getOrganizer() != null &&
                        camp.getOrganizer().getId().equals(staff.getOrganizer().getId())) {
                    allowed = true;
                }
            }

            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Camp not found: " + id);
            }
        }

        return toResponse(camp);
    }

    @Transactional
    public Camp updateCamp(Long id, CampRequest request) {
        Camp camp = getCampById(id);
        requireDraft(camp);
        copyDetails(camp, request);

        if (camp.getOrganizer() == null && request.organizerName() != null) {
            camp.setOrganizerName(request.organizerName().strip());
        }

        return campRepository.save(camp);
    }

    @Transactional
    public Camp updateCamp(Long id, CampRequest request, HttpServletRequest httpRequest) {
        Staff staff = staffAccessService.requireRole(httpRequest, Staff.Role.ORGANIZER);
        Camp camp = getCampById(id);
        staffAccessService.checkCampOwnership(staff, camp);
        requireDraft(camp);
        copyDetails(camp, request);

        return campRepository.save(camp);
    }

    @Transactional
    public void deleteCamp(Long id) {
        Camp camp = getCampById(id);
        requireDraft(camp);
        campRepository.delete(camp);
    }

    @Transactional
    public void deleteCamp(Long id, HttpServletRequest httpRequest) {
        Staff staff = staffAccessService.requireRole(httpRequest, Staff.Role.ORGANIZER);
        Camp camp = getCampById(id);
        staffAccessService.checkCampOwnership(staff, camp);
        requireDraft(camp);
        campRepository.delete(camp);
    }

    @Transactional
    public CampResponse submitCamp(Long id, HttpServletRequest httpRequest) {
        Staff staff = staffAccessService.requireRole(httpRequest, Staff.Role.ORGANIZER);
        Camp camp = getCampById(id);
        staffAccessService.checkCampOwnership(staff, camp);
        requireDraft(camp);

        List<CampSession> sessions = sessionRepository.findByCamp_IdOrderByStartTimeAsc(id);
        if (sessions.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Camp must have at least one service counter before submitting");
        }

        camp.setStatus("PENDING_REVIEW");
        camp.setRejectionReason(null);
        Camp saved = campRepository.save(camp);
        return toResponse(saved);
    }

    @Transactional
    public CampResponse reviewCamp(Long id, CampReviewRequest request, HttpServletRequest httpRequest) {
        Staff staff = staffAccessService.requireRole(httpRequest, Staff.Role.REVIEWER);
        Camp camp = getCampById(id);

        if (!camp.isPendingReview()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only camps pending review can be reviewed");
        }

        String action = request.action() != null ? request.action().toUpperCase().strip() : "";

        if ("APPROVE".equals(action)) {
            if (request.verificationSource() == null || request.verificationSource().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Official source or independent confirmation is required to approve a camp");
            }

            camp.setStatus("APPROVED");
            camp.setReviewedBy(staff);
            camp.setReviewedAt(LocalDateTime.now());
            camp.setVerificationSource(request.verificationSource().strip());
            camp.setRejectionReason(null);
        } else if ("RETURN_FOR_CORRECTIONS".equals(action) || "REJECT".equals(action)) {
            if (request.reason() == null || request.reason().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A reason is required when returning a camp for corrections");
            }

            camp.setStatus("DRAFT");
            camp.setReviewedBy(staff);
            camp.setReviewedAt(LocalDateTime.now());
            camp.setRejectionReason(request.reason().strip());
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid review action: " + request.action() + ". Use APPROVE or RETURN_FOR_CORRECTIONS");
        }

        Camp saved = campRepository.save(camp);
        return toResponse(saved);
    }

    @Transactional
    public void approveCamp(Long campId) {
        if (campRepository.approvePendingCamp(campId) == 1) {
            return;
        }

        if (!campRepository.existsById(campId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Camp not found");
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Only camps pending review can be approved");
    }

    @Transactional
    public CampResponse cancelCamp(Long id, HttpServletRequest httpRequest) {
        Staff staff = staffAccessService.requireRole(httpRequest, Staff.Role.ORGANIZER);
        Camp camp = getCampById(id);
        staffAccessService.checkCampOwnership(staff, camp);

        camp.setStatus("CANCELLED");
        Camp saved = campRepository.save(camp);
        return toResponse(saved);
    }

    public List<CampResponse> getPendingCamps() {
        return campRepository.findByStatus("PENDING_REVIEW").stream()
                .map(this::toResponse)
                .toList();
    }

    public CampResponse toResponse(Camp camp) {
        List<CampSession> sessions = sessionRepository.findByCamp_IdOrderByStartTimeAsc(camp.getId());
        return CampResponse.from(camp, sessions);
    }

    private void copyDetails(Camp camp, CampRequest request) {
        camp.setName(request.name().strip());
        camp.setCampDate(request.campDate());
        camp.setArea(request.area().strip());
        camp.setVenue(request.venue().strip());
    }

    private void requireDraft(Camp camp) {
        if (!camp.isDraft()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only draft camps can be updated or deleted");
        }
    }
}
