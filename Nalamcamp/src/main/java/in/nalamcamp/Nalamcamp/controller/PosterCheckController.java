package in.nalamcamp.Nalamcamp.controller;

import in.nalamcamp.Nalamcamp.dto.PosterCheckResponse;
import in.nalamcamp.Nalamcamp.dto.PosterMatchRequest;
import in.nalamcamp.Nalamcamp.dto.PosterReviewRequest;
import in.nalamcamp.Nalamcamp.service.PosterCheckService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/poster-checks")
public class PosterCheckController {

    private final PosterCheckService posterCheckService;

    public PosterCheckController(PosterCheckService posterCheckService) {
        this.posterCheckService = posterCheckService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PosterCheckResponse uploadPoster(
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam(name = "image", required = false) MultipartFile image) {

        MultipartFile target = file != null ? file : image;
        return posterCheckService.processPoster(target);
    }

    @PostMapping("/{id}/match")
    public PosterCheckResponse matchPoster(@PathVariable("id") String id,
                                           @RequestBody(required = false) PosterMatchRequest request) {
        return posterCheckService.matchPoster(id, request);
    }

    @PostMapping("/{id}/submit-review")
    public PosterCheckResponse submitForReview(@PathVariable("id") String id) {
        return posterCheckService.submitForReview(id);
    }

    @PatchMapping("/{id}/review")
    public PosterCheckResponse reviewPoster(@PathVariable("id") String id,
                                            @RequestBody PosterReviewRequest request,
                                            HttpServletRequest httpRequest) {
        return posterCheckService.reviewPoster(id, request, httpRequest);
    }
}
