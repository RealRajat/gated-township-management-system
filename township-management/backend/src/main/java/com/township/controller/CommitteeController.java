package com.township.controller;

import com.township.dto.*;
import com.township.service.CommitteeAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/committee")
@PreAuthorize("hasAnyAuthority('ROLE_COMMITTEE', 'ROLE_HELP_DESK')")
public class CommitteeController {

    private final CommitteeAnalyticsService analyticsService;

    public CommitteeController(CommitteeAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<CommitteeDashboardDto> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String towerId) {
        return ResponseEntity.ok(analyticsService.getDashboard(fromDate, toDate, towerId));
    }

    @GetMapping("/financial")
    public ResponseEntity<CommitteeFinancialKpiDto> getFinancialKpis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String towerId) {
        return ResponseEntity.ok(analyticsService.getFinancialKpis(fromDate, toDate, towerId));
    }

    @GetMapping("/trends")
    public ResponseEntity<List<CollectionTrendDto>> getCollectionTrends(
            @RequestParam(required = false) String towerId) {
        return ResponseEntity.ok(analyticsService.getCollectionTrends(towerId));
    }

    @GetMapping("/dues-ageing")
    public ResponseEntity<List<DuesAgeingBucketDto>> getDuesAgeing(
            @RequestParam(required = false) String towerId) {
        return ResponseEntity.ok(analyticsService.getDuesAgeing(towerId));
    }

    @GetMapping("/tower-dues")
    public ResponseEntity<List<TowerDuesSummaryDto>> getTowerDues() {
        return ResponseEntity.ok(analyticsService.getTowerDuesSummaries());
    }

    @GetMapping("/defaulters")
    public ResponseEntity<List<DefaulterSummaryDto>> getDefaulters(
            @RequestParam(required = false) String towerId) {
        return ResponseEntity.ok(analyticsService.getDefaulterSummaries(towerId));
    }

    @GetMapping("/complaints")
    public ResponseEntity<ComplaintAnalyticsDto> getComplaintAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String towerId) {
        return ResponseEntity.ok(analyticsService.getComplaintAnalytics(fromDate, toDate, towerId));
    }

    @GetMapping("/operational")
    public ResponseEntity<OperationalAnalyticsDto> getOperationalAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String towerId) {
        return ResponseEntity.ok(analyticsService.getOperationalAnalytics(fromDate, toDate, towerId));
    }

    // CSV Exports
    @GetMapping(value = "/reports/financial/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportFinancialCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String towerId) {
        byte[] csvData = analyticsService.generateFinancialCsv(fromDate, toDate, towerId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"financial-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping(value = "/reports/defaulters/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportDefaultersCsv(
            @RequestParam(required = false) String towerId) {
        byte[] csvData = analyticsService.generateDefaultersCsv(towerId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"defaulters-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping(value = "/reports/complaints/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportComplaintsCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String towerId) {
        byte[] csvData = analyticsService.generateComplaintsCsv(fromDate, toDate, towerId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"complaints-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping(value = "/reports/operational/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportOperationalCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String towerId) {
        byte[] csvData = analyticsService.generateOperationalCsv(fromDate, toDate, towerId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"operational-summary.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}

