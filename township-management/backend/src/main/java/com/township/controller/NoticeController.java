package com.township.controller;

import com.township.dto.*;
import com.township.entity.NoticeCategory;
import com.township.entity.NoticePriority;
import com.township.entity.NoticeStatus;
import com.township.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    // =========================================================================
    // RESIDENT & AUTHENTICATED ENDPOINTS
    // =========================================================================

    @GetMapping
    public ResponseEntity<List<NoticeDto>> getResidentNotices(
            @RequestParam(defaultValue = "false") boolean includeExpired,
            @RequestParam(required = false) NoticeCategory category,
            Authentication authentication) {
        return ResponseEntity.ok(noticeService.getResidentNotices(authentication.getName(), includeExpired, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoticeDto> getNoticeById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(noticeService.getNoticeByIdAndMarkRead(id, authentication.getName()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        noticeService.markAsRead(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        noticeService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/kpis")
    public ResponseEntity<NoticeOverviewKpiDto> getKpis(Authentication authentication) {
        return ResponseEntity.ok(noticeService.getOverviewKpis(authentication.getName()));
    }

    // =========================================================================
    // HELP DESK ADMINISTRATIVE ENDPOINTS (ROLE_HELP_DESK)
    // =========================================================================

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<List<NoticeDto>> getAllNoticesAdmin(
            @RequestParam(required = false) NoticeStatus status,
            @RequestParam(required = false) NoticeCategory category,
            @RequestParam(required = false) NoticePriority priority,
            @RequestParam(required = false) String towerId) {
        return ResponseEntity.ok(noticeService.getAllNoticesAdmin(status, category, priority, towerId));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<NoticeDto> createNotice(
            @Valid @RequestBody CreateNoticeRequest request,
            Authentication authentication) {
        NoticeDto created = noticeService.createNotice(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<NoticeDto> updateNotice(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNoticeRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(noticeService.updateNotice(id, request, authentication.getName()));
    }

    @PutMapping("/admin/{id}/pin")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<NoticeDto> togglePin(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(noticeService.togglePin(id, authentication.getName()));
    }

    @PutMapping("/admin/{id}/archive")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<NoticeDto> archiveNotice(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(noticeService.archiveNotice(id, authentication.getName()));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ROLE_HELP_DESK')")
    public ResponseEntity<Void> deleteNotice(
            @PathVariable Long id,
            Authentication authentication) {
        noticeService.deleteNotice(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}

