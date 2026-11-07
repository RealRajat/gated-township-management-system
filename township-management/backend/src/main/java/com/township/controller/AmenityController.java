package com.township.controller;

import com.township.dto.*;
import com.township.entity.BookingStatus;
import com.township.service.AmenityService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/amenities")
public class AmenityController {

    private final AmenityService amenityService;

    public AmenityController(AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    // =========================================================================
    // RESIDENT & AUTHENTICATED ENDPOINTS
    // =========================================================================

    @GetMapping
    public ResponseEntity<List<AmenityDto>> getActiveAmenities() {
        return ResponseEntity.ok(amenityService.getActiveAmenities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AmenityDto> getAmenityById(@PathVariable Long id) {
        return ResponseEntity.ok(amenityService.getAmenityById(id));
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<List<SlotAvailabilityDto>> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(amenityService.getAvailableSlots(id, date, username));
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingConfirmationDto> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            Authentication authentication) {
        BookingConfirmationDto confirmation = amenityService.createBooking(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(confirmation);
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingConfirmationDto>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(amenityService.getResidentBookings(authentication.getName()));
    }

    @GetMapping("/bookings/{reference}")
    public ResponseEntity<BookingConfirmationDto> getBookingByReference(
            @PathVariable String reference,
            Authentication authentication) {
        boolean isHelpDesk = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HELP_DESK"));
        return ResponseEntity.ok(amenityService.getBookingByReference(reference, authentication.getName(), isHelpDesk));
    }

    @PutMapping("/bookings/{id}/cancel")
    public ResponseEntity<BookingConfirmationDto> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) CancelBookingRequest request,
            Authentication authentication) {
        boolean isHelpDesk = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HELP_DESK"));
        return ResponseEntity.ok(amenityService.cancelBooking(id, request, authentication.getName(), isHelpDesk));
    }

    // =========================================================================
    // HELP DESK ADMINISTRATIVE ENDPOINTS (ROLE_HELP_DESK)
    // =========================================================================

    @GetMapping("/admin/overview")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<AmenityOverviewKpiDto> getOverviewKpis() {
        return ResponseEntity.ok(amenityService.getOverviewKpis());
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<List<AmenityDto>> getAllAmenitiesAdmin() {
        return ResponseEntity.ok(amenityService.getAllAmenities());
    }

    @PostMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<AmenityDto> createAmenity(
            @Valid @RequestBody CreateAmenityRequest request) {
        AmenityDto created = amenityService.createAmenity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<AmenityDto> updateAmenity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAmenityRequest request) {
        return ResponseEntity.ok(amenityService.updateAmenity(id, request));
    }

    @PutMapping("/admin/{id}/toggle-status")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<AmenityDto> toggleAmenityStatus(
            @PathVariable Long id,
            @Valid @RequestBody ToggleAmenityStatusRequest request) {
        return ResponseEntity.ok(amenityService.toggleAmenityStatus(id, request));
    }

    @GetMapping("/admin/bookings")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<List<BookingConfirmationDto>> getAllBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long amenityId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long flatId,
            Authentication authentication) {
        return ResponseEntity.ok(amenityService.getAllBookings(date, amenityId, status, flatId, authentication.getName()));
    }

    @PutMapping("/admin/bookings/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<BookingConfirmationDto> adminCancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) CancelBookingRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(amenityService.cancelBooking(id, request, authentication.getName(), true));
    }
}

