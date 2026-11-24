package in.nalamcamp.Nalamcamp.service;

import in.nalamcamp.Nalamcamp.Camp;
import in.nalamcamp.Nalamcamp.PosterCheck;
import in.nalamcamp.Nalamcamp.Staff;
import in.nalamcamp.Nalamcamp.dto.PosterCheckResponse;
import in.nalamcamp.Nalamcamp.dto.PosterMatchRequest;
import in.nalamcamp.Nalamcamp.dto.PosterReviewRequest;
import in.nalamcamp.Nalamcamp.repository.CampRepository;
import in.nalamcamp.Nalamcamp.repository.PosterCheckRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PosterCheckService {

    private final TesseractOcrService ocrService;
    private final PosterCheckRepository posterCheckRepository;
    private final CampRepository campRepository;
    private final StaffAccessService staffAccessService;

    public PosterCheckService(TesseractOcrService ocrService,
                              PosterCheckRepository posterCheckRepository,
                              CampRepository campRepository,
                              StaffAccessService staffAccessService) {
        this.ocrService = ocrService;
        this.posterCheckRepository = posterCheckRepository;
        this.campRepository = campRepository;
        this.staffAccessService = staffAccessService;
    }

    public PosterCheckResponse processPoster(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String lowerName = originalName.toLowerCase();

        boolean isJpegOrPng = (contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/jpg")))
                || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png");

        if (!isJpegOrPng) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid file type. Only JPEG and PNG image files are supported.");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "File size exceeds the 10MB limit.");
        }

        Path tempFile = null;
        String extractedText = "";

        try {
            String suffix = lowerName.endsWith(".png") ? ".png" : ".jpg";
            tempFile = Files.createTempFile("poster_upload_", suffix);
            file.transferTo(tempFile.toFile());

            extractedText = ocrService.extractText(tempFile);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded file: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }
        }

        TesseractOcrService.ParsedPosterFields fields = ocrService.parseFields(extractedText);

        PosterCheck posterCheck = new PosterCheck();
        String pcId = "PC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        posterCheck.setPosterCheckId(pcId);
        posterCheck.setOriginalFileName(originalName);
        posterCheck.setExtractedText(extractedText);
        posterCheck.setSuggestedOrganizer(fields.suggestedOrganizer());
        posterCheck.setSuggestedCampDate(fields.suggestedCampDate());
        posterCheck.setSuggestedVenue(fields.suggestedVenue());
        posterCheck.setSuggestedCampName(fields.suggestedCampName());
        posterCheck.setStatus(PosterCheck.PosterCheckStatus.EXTRACTED);
        posterCheck.setCreatedAt(LocalDateTime.now());

        PosterCheck saved = posterCheckRepository.save(posterCheck);
        return PosterCheckResponse.from(saved);
    }

    @Transactional
    public PosterCheckResponse matchPoster(String idOrPosterCheckId, PosterMatchRequest request) {
        PosterCheck posterCheck = findPosterCheck(idOrPosterCheckId);

        if (request != null) {
            if (request.organizerName() != null) posterCheck.setCorrectedOrganizer(request.organizerName().strip());
            if (request.campDate() != null) posterCheck.setCorrectedCampDate(request.campDate());
            if (request.venue() != null) posterCheck.setCorrectedVenue(request.venue().strip());
            if (request.campName() != null) posterCheck.setCorrectedCampName(request.campName().strip());
        }

        LocalDate targetDate = posterCheck.getCorrectedCampDate() != null ?
                posterCheck.getCorrectedCampDate() : posterCheck.getSuggestedCampDate();
        String targetOrganizer = posterCheck.getCorrectedOrganizer() != null ?
                posterCheck.getCorrectedOrganizer() : posterCheck.getSuggestedOrganizer();
        String targetVenue = posterCheck.getCorrectedVenue() != null ?
                posterCheck.getCorrectedVenue() : posterCheck.getSuggestedVenue();
        String targetCampName = posterCheck.getCorrectedCampName() != null ?
                posterCheck.getCorrectedCampName() : posterCheck.getSuggestedCampName();

        List<Camp> approvedCamps = campRepository.findByStatus("APPROVED");
        int highestScore = 0;
        List<Camp> bestCandidates = new ArrayList<>();

        for (Camp c : approvedCamps) {
            int score = 0;
            if (targetDate != null && c.getCampDate().equals(targetDate)) {
                score += 10;
            }
            if (targetCampName != null && !targetCampName.isBlank()) {
                if (c.getName().equalsIgnoreCase(targetCampName)) {
                    score += 10;
                } else if (c.getName().toLowerCase().contains(targetCampName.toLowerCase()) ||
                           targetCampName.toLowerCase().contains(c.getName().toLowerCase())) {
                    score += 5;
                }
            }
            if (targetVenue != null && !targetVenue.isBlank()) {
                if (c.getVenue().equalsIgnoreCase(targetVenue)) {
                    score += 10;
                } else if (c.getVenue().toLowerCase().contains(targetVenue.toLowerCase()) ||
                           targetVenue.toLowerCase().contains(c.getVenue().toLowerCase())) {
                    score += 5;
                }
            }
            if (targetOrganizer != null && !targetOrganizer.isBlank()) {
                if (c.getOrganizerName().equalsIgnoreCase(targetOrganizer)) {
                    score += 6;
                } else if (c.getOrganizerName().toLowerCase().contains(targetOrganizer.toLowerCase()) ||
                           targetOrganizer.toLowerCase().contains(c.getOrganizerName().toLowerCase())) {
                    score += 3;
                }
            }

            if (score > 0) {
                if (score > highestScore) {
                    highestScore = score;
                    bestCandidates.clear();
                    bestCandidates.add(c);
                } else if (score == highestScore) {
                    bestCandidates.add(c);
                }
            }
        }

        List<String> discrepanciesList = new ArrayList<>();

        if (bestCandidates.isEmpty() || highestScore < 5) {
            posterCheck.setMatchedCamp(null);
            posterCheck.setStatus(PosterCheck.PosterCheckStatus.UNABLE_TO_CONFIRM);
            posterCheck.setDiscrepancies("Unable to confirm poster details against approved camps. An unmatched poster can be submitted for manual review.");
        } else if (bestCandidates.size() > 1) {
            posterCheck.setMatchedCamp(null);
            posterCheck.setStatus(PosterCheck.PosterCheckStatus.UNABLE_TO_CONFIRM);
            posterCheck.setDiscrepancies("Unable to confirm: Multiple approved camps match the provided details with equal confidence. An unmatched poster can be submitted for manual review.");
        } else {
            Camp bestMatch = bestCandidates.get(0);
            posterCheck.setMatchedCamp(bestMatch);

            if (targetCampName != null && !targetCampName.isBlank()) {
                if (!bestMatch.getName().toLowerCase().contains(targetCampName.toLowerCase()) &&
                        !targetCampName.toLowerCase().contains(bestMatch.getName().toLowerCase())) {
                    discrepanciesList.add("Camp name discrepancy: poster indicates '" + targetCampName +
                            "', approved listing name is '" + bestMatch.getName() + "'");
                }
            }

            if (targetOrganizer != null && !targetOrganizer.isBlank()) {
                if (!bestMatch.getOrganizerName().toLowerCase().contains(targetOrganizer.toLowerCase()) &&
                        !targetOrganizer.toLowerCase().contains(bestMatch.getOrganizerName().toLowerCase())) {
                    discrepanciesList.add("Organizer discrepancy: poster indicates '" + targetOrganizer +
                            "', approved listing is '" + bestMatch.getOrganizerName() + "'");
                }
            }

            if (targetVenue != null && !targetVenue.isBlank()) {
                if (!bestMatch.getVenue().toLowerCase().contains(targetVenue.toLowerCase()) &&
                        !targetVenue.toLowerCase().contains(bestMatch.getVenue().toLowerCase())) {
                    discrepanciesList.add("Venue discrepancy: poster indicates '" + targetVenue +
                            "', approved listing venue is '" + bestMatch.getVenue() + "'");
                }
            }

            if (targetDate != null && !bestMatch.getCampDate().equals(targetDate)) {
                discrepanciesList.add("Date discrepancy: poster indicates '" + targetDate +
                        "', approved listing date is '" + bestMatch.getCampDate() + "'");
            }

            if (discrepanciesList.isEmpty()) {
                posterCheck.setStatus(PosterCheck.PosterCheckStatus.MATCHED);
                posterCheck.setDiscrepancies(null);
            } else {
                posterCheck.setStatus(PosterCheck.PosterCheckStatus.DISCREPANCIES);
                posterCheck.setDiscrepancies(String.join("; ", discrepanciesList));
            }
        }

        PosterCheck saved = posterCheckRepository.save(posterCheck);
        return PosterCheckResponse.from(saved);
    }

    @Transactional
    public PosterCheckResponse submitForReview(String idOrPosterCheckId) {
        PosterCheck posterCheck = findPosterCheck(idOrPosterCheckId);
        posterCheck.setStatus(PosterCheck.PosterCheckStatus.PENDING_REVIEW);
        PosterCheck saved = posterCheckRepository.save(posterCheck);
        return PosterCheckResponse.from(saved);
    }

    @Transactional
    public PosterCheckResponse reviewPoster(String idOrPosterCheckId, PosterReviewRequest request, HttpServletRequest httpRequest) {
        Staff staff = staffAccessService.requireRole(httpRequest, Staff.Role.REVIEWER);
        PosterCheck posterCheck = findPosterCheck(idOrPosterCheckId);

        if (request.campId() != null) {
            Camp camp = campRepository.findById(request.campId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camp not found: " + request.campId()));
            posterCheck.setMatchedCamp(camp);
        }

        if (request.matchStatus() != null) {
            posterCheck.setStatus(request.matchStatus());
        }

        if (request.reviewNotes() != null) {
            posterCheck.setReviewNotes(request.reviewNotes().strip());
        }

        posterCheck.setReviewedBy(staff);
        posterCheck.setReviewedAt(LocalDateTime.now());

        PosterCheck saved = posterCheckRepository.save(posterCheck);
        return PosterCheckResponse.from(saved);
    }

    public List<PosterCheckResponse> getPendingPosters() {
        return posterCheckRepository.findByStatus(PosterCheck.PosterCheckStatus.PENDING_REVIEW)
                .stream()
                .map(PosterCheckResponse::from)
                .toList();
    }

    public PosterCheck findPosterCheck(String idOrPosterCheckId) {
        try {
            Long numericId = Long.parseLong(idOrPosterCheckId);
            return posterCheckRepository.findById(numericId)
                    .or(() -> posterCheckRepository.findByPosterCheckId(idOrPosterCheckId))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Poster check record not found: " + idOrPosterCheckId));
        } catch (NumberFormatException e) {
            return posterCheckRepository.findByPosterCheckId(idOrPosterCheckId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Poster check record not found: " + idOrPosterCheckId));
        }
    }
}

