package in.nalamcamp.Nalamcamp.controller;

import in.nalamcamp.Nalamcamp.Camp;
import in.nalamcamp.Nalamcamp.dto.CampRequest;
import in.nalamcamp.Nalamcamp.dto.CampResponse;
import in.nalamcamp.Nalamcamp.dto.CampStatusRequest;
import in.nalamcamp.Nalamcamp.service.CampService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/camps")
public class CampController {

    private final CampService campService;

    public CampController(CampService campService) {
        this.campService = campService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampResponse createCamp(@Valid @RequestBody CampRequest request,
                                   HttpServletRequest httpRequest) {
        Camp camp = campService.createCamp(request, httpRequest);
        return campService.toResponse(camp);
    }

    @GetMapping
    public List<CampResponse> getCamps(
            @RequestParam(name = "area", required = false) String area,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "service", required = false) String service) {
        return campService.getPublicCamps(area, date, service);
    }

    @GetMapping("/{id}")
    public CampResponse getCampById(@PathVariable("id") Long id,
                                    HttpServletRequest httpRequest) {
        return campService.getCampResponseById(id, httpRequest);
    }

    @PutMapping("/{id}")
    public CampResponse updateCamp(@PathVariable("id") Long id,
                                   @Valid @RequestBody CampRequest request,
                                   HttpServletRequest httpRequest) {
        Camp updated = campService.updateCamp(id, request, httpRequest);
        return campService.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCamp(@PathVariable("id") Long id,
                           HttpServletRequest httpRequest) {
        campService.deleteCamp(id, httpRequest);
    }

    @PostMapping("/{id}/submit")
    public CampResponse submitCamp(@PathVariable("id") Long id,
                                   HttpServletRequest httpRequest) {
        return campService.submitCamp(id, httpRequest);
    }

    @PatchMapping("/{id}/status")
    public CampResponse updateCampStatus(@PathVariable("id") Long id,
                                         @RequestBody(required = false) CampStatusRequest request,
                                         HttpServletRequest httpRequest) {
        return campService.cancelCamp(id, httpRequest);
    }
}
