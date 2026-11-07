package com.township.controller;

import com.township.dto.HealthResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${app.name:Gated Township Resident Management System}")
    private String appName;

    @Value("${app.version:1.0.0-SNAPSHOT}")
    private String appVersion;

    @Value("${app.environment:dev}")
    private String environment;

    private final Optional<DataSource> dataSource;

    @Autowired
    public HealthController(Optional<DataSource> dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponseDto> getHealth() {
        Map<String, Object> dbInfo = new HashMap<>();
        dbInfo.put("type", "PostgreSQL");

        boolean dbConnected = false;
        if (dataSource.isPresent()) {
            try (Connection conn = dataSource.get().getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                dbInfo.put("status", "CONNECTED");
                dbInfo.put("productName", metaData.getDatabaseProductName());
                dbInfo.put("productVersion", metaData.getDatabaseProductVersion());
                dbConnected = true;
            } catch (Exception ex) {
                dbInfo.put("status", "DISCONNECTED");
                dbInfo.put("error", "Could not connect to PostgreSQL: " + ex.getMessage());
                dbInfo.put("suggestion", "Ensure PostgreSQL is installed, running on port 5432, and database 'township_db' exists.");
            }
        } else {
            dbInfo.put("status", "NOT_CONFIGURED");
            dbInfo.put("error", "No DataSource bean detected in application context.");
        }

        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("jvmFreeMemoryMB", Runtime.getRuntime().freeMemory() / (1024 * 1024));
        systemInfo.put("jvmTotalMemoryMB", Runtime.getRuntime().totalMemory() / (1024 * 1024));

        String overallStatus = dbConnected ? "UP" : "DEGRADED";

        HealthResponseDto response = new HealthResponseDto(
                overallStatus,
                appName,
                appVersion,
                environment,
                Instant.now(),
                dbInfo,
                systemInfo
        );

        return ResponseEntity.ok(response);
    }
}

