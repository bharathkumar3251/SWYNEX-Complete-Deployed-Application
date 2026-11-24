package in.nalamcamp.Nalamcamp.controller;

import in.nalamcamp.Nalamcamp.Staff;
import in.nalamcamp.Nalamcamp.dto.CampResponse;
import in.nalamcamp.Nalamcamp.dto.CampReviewRequest;
import in.nalamcamp.Nalamcamp.service.CampService;
import in.nalamcamp.Nalamcamp.service.StaffAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/camps")
public class CampReviewController {

    private final CampService campService;
    private final StaffAccessService staffAccessService;

    public CampReviewController(CampService campService,
                                StaffAccessService staffAccessService) {
        this.campService = campService;
        this.staffAccessService = staffAccessService;
    }

    @PatchMapping("/{campId}/review")
    public CampResponse review(@PathVariable Long campId,
                               @RequestBody CampReviewRequest request,
                               HttpServletRequest httpRequest) {
        return campService.reviewCamp(campId, request, httpRequest);
    }

    @PostMapping("/{campId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable Long campId, HttpServletRequest request) {
        staffAccessService.requireRole(request, Staff.Role.REVIEWER);
        campService.approveCamp(campId);
    }
}
