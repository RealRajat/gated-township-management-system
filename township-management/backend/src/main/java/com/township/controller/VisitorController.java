package com.township.controller;

import com.township.dto.*;
import com.township.entity.VisitorStatus;
import com.township.entity.VisitorType;
import com.township.service.VisitorService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/visitors")
public class VisitorController {

    private final VisitorService visitorService;

    public VisitorController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    // =========================================================================
    // RESIDENT ENDPOINTS
    // =========================================================================

    /**
     * Resident: Pre-approve an upcoming visitor.
     */
    @PostMapping("/pre-approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VisitorDto> preApproveVisitor(
            @Valid @RequestBody PreApproveVisitorRequest request,
            Authentication authentication) {
        VisitorDto dto = visitorService.preApproveVisitor(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Resident: View all visitors associated with resident's flats.
     */
    @GetMapping("/my-visitors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VisitorDto>> getMyVisitors(
            @RequestParam(required = false) VisitorStatus status,
            Authentication authentication) {
        return ResponseEntity.ok(visitorService.getMyVisitors(authentication.getName(), status));
    }

    /**
     * Resident: Get pending approval requests awaiting resident action.
     */
    @GetMapping("/my-pending-approvals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VisitorDto>> getMyPendingApprovals(Authentication authentication) {
        return ResponseEntity.ok(visitorService.getMyPendingApprovals(authentication.getName()));
    }

    /**
     * Resident: Approve or Reject a walk-in visitor request.
     */
    @PutMapping("/{id}/decision")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VisitorDto> processResidentApproval(
            @PathVariable Long id,
            @Valid @RequestBody ResidentApprovalDecisionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(visitorService.processResidentApproval(id, request, authentication.getName()));
    }

    /**
     * Resident: Revoke an unused visitor pass.
     */
    @PutMapping("/{id}/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VisitorDto> revokePass(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(visitorService.revokePass(id, authentication.getName()));
    }

    /**
     * View visitor details by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VisitorDto> getVisitorById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(visitorService.getVisitorById(id, authentication.getName()));
    }

    /**
     * Securely stream visitor photo.
     */
    @GetMapping("/{id}/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> getVisitorPhoto(
            @PathVariable Long id,
            Authentication authentication) {
        Resource resource = visitorService.getVisitorPhoto(id, authentication.getName());

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

    // =========================================================================
    // GATE GUARD ENDPOINTS
    // =========================================================================

    /**
     * Gate Guard: Lookup visitor by pass code, phone, or QR token.
     */
    @GetMapping("/gate/lookup")
    @PreAuthorize("hasAnyAuthority('ROLE_GUARD', 'ROLE_HELP_DESK')")
    public ResponseEntity<List<VisitorDto>> lookupVisitor(@RequestParam String q) {
        return ResponseEntity.ok(visitorService.lookupVisitor(q));
    }

    /**
     * Gate Guard: Register unapproved walk-in visitor (JSON).
     */
    @PostMapping(value = "/gate/walk-in", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_GUARD', 'ROLE_HELP_DESK')")
    public ResponseEntity<VisitorDto> registerWalkInJson(
            @Valid @RequestBody WalkInVisitorRequest request,
            Authentication authentication) {
        VisitorDto dto = visitorService.registerWalkInVisitor(request, null, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Gate Guard: Register unapproved walk-in visitor with optional photo (Multipart).
     */
    @PostMapping(value = "/gate/walk-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_GUARD', 'ROLE_HELP_DESK')")
    public ResponseEntity<VisitorDto> registerWalkInMultipart(
            @RequestParam("flatId") Long flatId,
            @RequestParam("visitorName") String visitorName,
            @RequestParam("visitorPhone") String visitorPhone,
            @RequestParam("visitorType") VisitorType visitorType,
            @RequestParam(value = "purpose", required = false) String purpose,
            @RequestParam(value = "vehicleNumber", required = false) String vehicleNumber,
            @RequestParam(value = "gate", required = false) String gate,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {
        WalkInVisitorRequest request = new WalkInVisitorRequest(flatId, visitorName, visitorPhone, visitorType, purpose, vehicleNumber, gate);
        VisitorDto dto = visitorService.registerWalkInVisitor(request, photo, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Gate Guard: Check-In verified visitor (JSON).
     */
    @PostMapping(value = "/gate/check-in", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_GUARD', 'ROLE_HELP_DESK')")
    public ResponseEntity<VisitorDto> checkInJson(
            @Valid @RequestBody GateCheckInRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(visitorService.checkInVisitor(request, null, authentication.getName()));
    }

    /**
     * Gate Guard: Check-In verified visitor with optional photo (Multipart).
     */
    @PostMapping(value = "/gate/check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_GUARD', 'ROLE_HELP_DESK')")
    public ResponseEntity<VisitorDto> checkInMultipart(
            @RequestParam("passCodeOrToken") String passCodeOrToken,
            @RequestParam(value = "gate", required = false) String gate,
            @RequestParam(value = "vehicleNumber", required = false) String vehicleNumber,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {
        GateCheckInRequest request = new GateCheckInRequest(passCodeOrToken, gate, vehicleNumber);
        return ResponseEntity.ok(visitorService.checkInVisitor(request, photo, authentication.getName()));
    }

    /**
     * Gate Guard: Check-Out visitor exiting the township.
     */
    @PutMapping("/gate/check-out/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_GUARD', 'ROLE_HELP_DESK')")
    public ResponseEntity<VisitorDto> checkOutVisitor(
            @PathVariable Long id,
            @RequestBody(required = false) GateCheckOutRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(visitorService.checkOutVisitor(id, request, authentication.getName()));
    }

    /**
     * Offline Sync: Batch ingest offline check-in and check-out events idempotently.
     */
    @PostMapping("/gate/sync")
    @PreAuthorize("hasAnyAuthority('ROLE_GUARD', 'ROLE_HELP_DESK')")
    public ResponseEntity<OfflineSyncBatchResponse> syncOfflineEvents(
            @Valid @RequestBody OfflineSyncBatchRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(visitorService.syncOfflineEvents(request, authentication.getName()));
    }

    /**
     * Gate Guard: Download minimal active passes cache for offline gate tolerance.
     */
    @GetMapping("/gate/active-passes")
    @PreAuthorize("hasAnyAuthority('ROLE_GUARD', 'ROLE_HELP_DESK')")
    public ResponseEntity<List<GateActivePassDto>> getActivePassesForOfflineCache() {
        return ResponseEntity.ok(visitorService.getActivePassesForOfflineCache());
    }

    /**
     * Gate / Help Desk: Get live gate KPI overview.
     */
    @GetMapping("/gate/overview")
    @PreAuthorize("hasAnyAuthority('ROLE_GUARD', 'ROLE_HELP_DESK')")
    public ResponseEntity<GateOverviewKpiDto> getGateOverview() {
        return ResponseEntity.ok(visitorService.getGateOverview());
    }

    // =========================================================================
    // HELP DESK / ADMIN ENDPOINTS
    // =========================================================================

    /**
     * Help Desk: Search and filter township-wide visitors.
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<List<VisitorDto>> getAllVisitors(
            @RequestParam(required = false) VisitorStatus status,
            @RequestParam(required = false) Long flatId,
            @RequestParam(required = false) String towerId) {
        return ResponseEntity.ok(visitorService.getAllVisitorsForHelpDesk(status, flatId, towerId));
    }

    /**
     * Help Desk: Trigger 90-day privacy retention cleanup manually.
     */
    @PostMapping("/admin/cleanup-photos")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<Map<String, Object>> cleanupExpiredPhotos() {
        int count = visitorService.cleanupExpiredVisitorPhotos();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "purgedPhotoCount", count,
                "retentionPolicy", "90-Day Privacy Retention"
        ));
    }
}

