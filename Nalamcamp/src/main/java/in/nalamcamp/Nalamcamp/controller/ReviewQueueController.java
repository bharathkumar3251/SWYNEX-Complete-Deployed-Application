package in.nalamcamp.Nalamcamp.controller;

import in.nalamcamp.Nalamcamp.Staff;
import in.nalamcamp.Nalamcamp.dto.CampResponse;
import in.nalamcamp.Nalamcamp.dto.PosterCheckResponse;
import in.nalamcamp.Nalamcamp.dto.ReviewQueueResponse;
import in.nalamcamp.Nalamcamp.service.CampService;
import in.nalamcamp.Nalamcamp.service.PosterCheckService;
import in.nalamcamp.Nalamcamp.service.StaffAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/review-queue")
public class ReviewQueueController {

    private final CampService campService;
    private final PosterCheckService posterCheckService;
    private final StaffAccessService staffAccessService;

    public ReviewQueueController(CampService campService,
                                 PosterCheckService posterCheckService,
                                 StaffAccessService staffAccessService) {
        this.campService = campService;
        this.posterCheckService = posterCheckService;
        this.staffAccessService = staffAccessService;
    }

    @GetMapping
    public ReviewQueueResponse getReviewQueue(HttpServletRequest request) {
        staffAccessService.requireRole(request, Staff.Role.REVIEWER);

        List<CampResponse> pendingCamps = campService.getPendingCamps();
        List<PosterCheckResponse> pendingPosters = posterCheckService.getPendingPosters();

        return new ReviewQueueResponse(pendingCamps, pendingPosters);
    }
}
