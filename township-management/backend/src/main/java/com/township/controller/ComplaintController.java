package com.township.controller;

import com.township.dto.*;
import com.township.entity.ComplaintCategory;
import com.township.entity.ComplaintPriority;
import com.township.entity.ComplaintStatus;
import com.township.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * Preview AI and Safety Classification before filing
     */
    @PostMapping("/preview-classify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComplaintClassificationDto> previewClassification(
            @Valid @RequestBody ComplaintPreviewRequest request) {
        return ResponseEntity.ok(complaintService.previewClassification(request));
    }

    /**
     * File a complaint (JSON format without photo)
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComplaintDetailDto> createComplaintJson(
            @Valid @RequestBody CreateComplaintRequest request,
            Authentication authentication) {
        ComplaintDetailDto dto = complaintService.createComplaint(request, null, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * File a complaint (Multipart format with optional photo attachment)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComplaintDetailDto> createComplaintMultipart(
            @RequestParam("flatId") Long flatId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "category", required = false) ComplaintCategory category,
            @RequestParam(value = "priority", required = false) ComplaintPriority priority,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {
        CreateComplaintRequest request = new CreateComplaintRequest(flatId, title, description, category, priority);
        ComplaintDetailDto dto = complaintService.createComplaint(request, photo, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Get resident's complaints for their apartments
     */
    @GetMapping("/my-complaints")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ComplaintSummaryDto>> getMyComplaints(Authentication authentication) {
        return ResponseEntity.ok(complaintService.getResidentComplaints(authentication.getName()));
    }

    /**
     * Get complaint details by ID (enforcing resident vs help desk permissions)
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComplaintDetailDto> getComplaintById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(complaintService.getComplaintById(id, authentication.getName()));
    }

    /**
     * Securely stream complaint photo (resident author or Help Desk only)
     */
    @GetMapping("/{id}/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> getComplaintPhoto(
            @PathVariable Long id,
            Authentication authentication) {
        Resource resource = complaintService.getComplaintPhoto(id, authentication.getName());

        String contentType = "image/jpeg";
        try {
            String probe = Files.probeContentType(Paths.get(resource.getFile().getAbsolutePath()));
            if (probe != null) contentType = probe;
        } catch (IOException ignored) {}

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .body(resource);
    }

    /**
     * Resident rate complaint after resolution
     */
    @PostMapping("/{id}/rate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComplaintDetailDto> rateComplaint(
            @PathVariable Long id,
            @Valid @RequestBody RateComplaintRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(complaintService.rateComplaint(id, request, authentication.getName()));
    }

    // =========================================================================
    // HELP DESK / ADMIN ENDPOINTS
    // =========================================================================

    /**
     * Help Desk: view all complaints with filters
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<List<ComplaintSummaryDto>> getAllComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) ComplaintPriority priority,
            @RequestParam(required = false) String towerId,
            @RequestParam(required = false) Boolean overdueOnly) {
        return ResponseEntity.ok(complaintService.getAllComplaintsForHelpDesk(status, category, priority, towerId, overdueOnly));
    }

    /**
     * Help Desk: Overview KPI metrics
     */
    @GetMapping("/admin/overview")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<ComplaintOverviewKpiDto> getHelpDeskOverview() {
        return ResponseEntity.ok(complaintService.getHelpDeskOverview());
    }

    /**
     * Help Desk: Assign complaint to team or staff member
     */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<ComplaintDetailDto> assignComplaint(
            @PathVariable Long id,
            @Valid @RequestBody AssignComplaintRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(complaintService.assignComplaint(id, request, authentication.getName()));
    }

    /**
     * Help Desk: Update complaint status and resolution notes
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<ComplaintDetailDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateComplaintStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, request, authentication.getName()));
    }

    /**
     * Help Desk: Add staff internal note
     */
    @PostMapping("/{id}/internal-note")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<ComplaintDetailDto> addInternalNote(
            @PathVariable Long id,
            @Valid @RequestBody AddInternalNoteRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(complaintService.addInternalNote(id, request, authentication.getName()));
    }

    /**
     * Help Desk: Link duplicate complaint without deleting
     */
    @PostMapping("/{id}/link-duplicate")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<ComplaintDetailDto> linkDuplicate(
            @PathVariable Long id,
            @Valid @RequestBody LinkDuplicateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(complaintService.linkDuplicate(id, request, authentication.getName()));
    }
}

