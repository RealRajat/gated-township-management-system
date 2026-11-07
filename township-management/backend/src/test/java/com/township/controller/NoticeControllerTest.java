package com.township.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.township.dto.CreateNoticeRequest;
import com.township.dto.LoginRequest;
import com.township.dto.UpdateNoticeRequest;
import com.township.entity.NoticeCategory;
import com.township.entity.NoticePriority;
import com.township.entity.NoticeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoticeControllerTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final HttpClient client = HttpClient.newHttpClient();

    private String neerajToken;   // Tower T07
    private String swatiToken;    // Tower T12
    private String aamirToken;    // Tower T21
    private String guardToken;
    private String helpDeskToken;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @BeforeEach
    void setupTokens() throws Exception {
        // Resident 1: Neeraj Kulkarni (Flat 1, T07-1203 -> Tower T07)
        this.neerajToken = authenticateAndGetToken("neeraj.kulkarni@grandvistapune.in", "Resident@123");

        // Resident 2: Swati Joshi (Flat 2, T12-0408 -> Tower T12)
        this.swatiToken = authenticateAndGetToken("swati.joshi@grandvistapune.in", "Resident@123");

        // Resident 3: Aamir Shaikh (Flat 3, T21-1601 -> Tower T21)
        this.aamirToken = authenticateAndGetToken("aamir.shaikh@grandvistapune.in", "Resident@123");

        // Guard: Santosh Shinde
        this.guardToken = authenticateAndGetToken("guard@grandvistapune.in", "Guard@123");

        // Help Desk: Col. Rajiv Sharma
        this.helpDeskToken = authenticateAndGetToken("helpdesk@grandvistapune.in", "HelpDesk@123");
    }

    private String authenticateAndGetToken(String identifier, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(identifier, password);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(loginRequest)))
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Login should succeed: " + res.body());
        JsonNode root = objectMapper.readTree(res.body());
        return root.get("token").asText();
    }

    @Test
    @DisplayName("1. Resident Can View Township-Wide and Their Tower Notices")
    void testResidentCanViewTownshipAndTowerNotices() throws Exception {
        // Neeraj belongs to Tower T07
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());

        JsonNode notices = objectMapper.readTree(res.body());
        assertTrue(notices.isArray());
        assertTrue(notices.size() >= 4, "Should receive township notices + T07 notices");

        boolean foundT07Notice = false;
        boolean foundT12Notice = false;
        boolean foundEmergencyNotice = false;

        for (JsonNode n : notices) {
            String title = n.get("title").asText();
            String towerId = n.has("targetTowerId") && !n.get("targetTowerId").isNull() ? n.get("targetTowerId").asText() : null;

            if ("T07".equals(towerId)) {
                foundT07Notice = true;
            }
            if ("T12".equals(towerId)) {
                foundT12Notice = true;
            }
            if ("EMERGENCY".equals(n.get("category").asText())) {
                foundEmergencyNotice = true;
                assertTrue(n.get("isPinned").asBoolean(), "Emergency notice should be pinned");
                assertEquals("URGENT", n.get("priority").asText());
            }
        }

        assertTrue(foundT07Notice, "Neeraj (Tower T07) should see Tower T07 notices");
        assertFalse(foundT12Notice, "Neeraj (Tower T07) should NOT see Tower T12 notices");
        assertTrue(foundEmergencyNotice, "Neeraj should see township-wide emergency notice");
    }

    @Test
    @DisplayName("2. Tower-Targeting Isolation between Different Towers")
    void testTowerTargetingIsolation() throws Exception {
        // Swati belongs to Tower T12
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices"))
                .header("Authorization", "Bearer " + swatiToken)
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());

        JsonNode notices = objectMapper.readTree(res.body());
        boolean foundT12Notice = false;
        boolean foundT07Notice = false;

        for (JsonNode n : notices) {
            String towerId = n.has("targetTowerId") && !n.get("targetTowerId").isNull() ? n.get("targetTowerId").asText() : null;
            if ("T12".equals(towerId)) foundT12Notice = true;
            if ("T07".equals(towerId)) foundT07Notice = true;
        }

        assertTrue(foundT12Notice, "Swati (Tower T12) should see Tower T12 notices");
        assertFalse(foundT07Notice, "Swati (Tower T12) should NOT see Tower T07 notices");
    }

    @Test
    @DisplayName("3. Read Tracking Lifecycle and Auto-Mark on Open")
    void testReadTrackingLifecycle() throws Exception {
        // Neeraj gets list of notices
        HttpRequest listReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> listRes = client.send(listReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, listRes.statusCode());
        JsonNode notices = objectMapper.readTree(listRes.body());

        // Find an unread notice or use the first one
        JsonNode targetNotice = notices.get(0);
        Long noticeId = targetNotice.get("id").asLong();

        // Open details GET /api/notices/{id}
        HttpRequest detailReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/" + noticeId))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> detailRes = client.send(detailReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, detailRes.statusCode());

        JsonNode detail = objectMapper.readTree(detailRes.body());
        assertTrue(detail.get("isRead").asBoolean(), "Notice should now be marked as read");

        // Verify that subsequent list fetch also shows it as read
        HttpResponse<String> listRes2 = client.send(listReq, HttpResponse.BodyHandlers.ofString());
        JsonNode notices2 = objectMapper.readTree(listRes2.body());
        for (JsonNode n : notices2) {
            if (n.get("id").asLong() == noticeId) {
                assertTrue(n.get("isRead").asBoolean());
            }
        }
    }

    @Test
    @DisplayName("4. Mark All As Read Workflow")
    void testMarkAllAsRead() throws Exception {
        // Aamir marks all visible notices as read
        HttpRequest readAllReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/read-all"))
                .header("Authorization", "Bearer " + aamirToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> readAllRes = client.send(readAllReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, readAllRes.statusCode());

        // Check KPIs: unread count should be 0
        HttpRequest kpiReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/kpis"))
                .header("Authorization", "Bearer " + aamirToken)
                .GET()
                .build();
        HttpResponse<String> kpiRes = client.send(kpiReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, kpiRes.statusCode());
        JsonNode kpi = objectMapper.readTree(kpiRes.body());
        assertEquals(0, kpi.get("unreadCount").asLong(), "After mark-all-read, unread count must be 0");
    }

    @Test
    @DisplayName("5. Help Desk Notice Creation, Pinning, and Archiving")
    void testHelpDeskNoticeLifecycle() throws Exception {
        // 1. Create notice
        CreateNoticeRequest createReq = new CreateNoticeRequest(
                "Scheduled Rooftop Solar Panel Cleaning",
                "Solar panel maintenance technicians will be cleaning photovoltaic arrays across all 28 towers this Saturday from 08:00 AM.",
                NoticeCategory.MAINTENANCE,
                NoticePriority.HIGH,
                NoticeStatus.PUBLISHED,
                null, // Township-wide
                false,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7)
        );

        HttpRequest createHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/admin"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(createReq)))
                .build();
        HttpResponse<String> createRes = client.send(createHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createRes.statusCode(), "Creation should succeed: " + createRes.body());

        Long noticeId = objectMapper.readTree(createRes.body()).get("id").asLong();

        // 2. Toggle Pin
        HttpRequest pinHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/admin/" + noticeId + "/pin"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> pinRes = client.send(pinHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pinRes.statusCode());
        assertTrue(objectMapper.readTree(pinRes.body()).get("isPinned").asBoolean(), "Notice should be pinned");

        // 3. Update notice
        UpdateNoticeRequest updateReq = new UpdateNoticeRequest(
                "Updated: Scheduled Rooftop Solar Panel Cleaning",
                "Updated cleaning schedule: Block A & B on Saturday, Block C & D on Sunday.",
                NoticeCategory.MAINTENANCE,
                NoticePriority.HIGH,
                NoticeStatus.PUBLISHED,
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7)
        );
        HttpRequest updateHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/admin/" + noticeId))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(updateReq)))
                .build();
        HttpResponse<String> updateRes = client.send(updateHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, updateRes.statusCode());
        assertEquals("Updated: Scheduled Rooftop Solar Panel Cleaning", objectMapper.readTree(updateRes.body()).get("title").asText());

        // 4. Archive notice
        HttpRequest archiveHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/admin/" + noticeId + "/archive"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> archiveRes = client.send(archiveHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, archiveRes.statusCode());
        assertEquals("ARCHIVED", objectMapper.readTree(archiveRes.body()).get("status").asText());

        // 5. Verify archived notice disappears from resident feed
        HttpRequest resReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> resFeed = client.send(resReq, HttpResponse.BodyHandlers.ofString());
        assertFalse(resFeed.body().contains("Scheduled Rooftop Solar Panel Cleaning"), "Archived notice should not be in resident feed");
    }

    @Test
    @DisplayName("6. Notice Expiry Handling")
    void testNoticeExpiryHandling() throws Exception {
        // Create an already-expired notice
        CreateNoticeRequest expiredReq = new CreateNoticeRequest(
                "Flash Sale at Organic Garden Stall",
                "Fresh organic fruits and vegetables available at Central Lawn.",
                NoticeCategory.EVENT,
                NoticePriority.LOW,
                NoticeStatus.PUBLISHED,
                null,
                false,
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(1) // Expired yesterday
        );

        HttpRequest createHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/admin"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(expiredReq)))
                .build();
        HttpResponse<String> createRes = client.send(createHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createRes.statusCode());

        // Default resident feed (includeExpired = false) should NOT contain it
        HttpRequest activeReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices?includeExpired=false"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> activeRes = client.send(activeReq, HttpResponse.BodyHandlers.ofString());
        assertFalse(activeRes.body().contains("Flash Sale at Organic Garden Stall"), "Active feed should omit expired notices");

        // Request with includeExpired = true SHOULD contain it
        HttpRequest allReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices?includeExpired=true"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> allRes = client.send(allReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(allRes.body().contains("Flash Sale at Organic Garden Stall"), "includeExpired=true should return expired notice");
    }

    @Test
    @DisplayName("7. Security: Resident Cannot Access Other Towers Notice Details Directly")
    void testCrossTowerDetailAccessForbidden() throws Exception {
        // Help Desk creates a Tower T21 specific notice
        CreateNoticeRequest t21Req = new CreateNoticeRequest(
                "Tower Q (Bluebell / T21): Water Tank Chemical Sterilization",
                "Private water tank cleaning for Tower T21 residents only.",
                NoticeCategory.MAINTENANCE,
                NoticePriority.NORMAL,
                NoticeStatus.PUBLISHED,
                "T21",
                false,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(5)
        );

        HttpRequest createHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/admin"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(t21Req)))
                .build();
        HttpResponse<String> createRes = client.send(createHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createRes.statusCode());
        Long t21NoticeId = objectMapper.readTree(createRes.body()).get("id").asLong();

        // Neeraj (Tower T07) attempts to fetch details of Tower T21 notice
        HttpRequest fraudReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/" + t21NoticeId))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> fraudRes = client.send(fraudReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, fraudRes.statusCode(), "Cross-tower direct detail access must be rejected with 403 Forbidden");
    }

    @Test
    @DisplayName("8. Security: Resident and Guard Cannot Access Admin Endpoints")
    void testAdminEndpointsForbiddenForNonHelpDesk() throws Exception {
        CreateNoticeRequest req = new CreateNoticeRequest(
                "Fake Resident Notice", "Unauthorized text", NoticeCategory.GENERAL, NoticePriority.LOW,
                NoticeStatus.PUBLISHED, null, false, LocalDateTime.now(), null
        );

        // Resident attempting to create notice
        HttpRequest resCreate = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/admin"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(req)))
                .build();
        HttpResponse<String> resCreateRes = client.send(resCreate, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, resCreateRes.statusCode(), "Resident must be forbidden from notice creation");

        // Guard attempting to create notice
        HttpRequest guardCreate = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/admin"))
                .header("Authorization", "Bearer " + guardToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(req)))
                .build();
        HttpResponse<String> guardCreateRes = client.send(guardCreate, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, guardCreateRes.statusCode(), "Guard must be forbidden from notice creation");
    }

    @Test
    @DisplayName("9. Input Validation & Invalid Date Rejection")
    void testInputValidationRejections() throws Exception {
        // Expiry date before publish date
        CreateNoticeRequest invalidDates = new CreateNoticeRequest(
                "Invalid Notice",
                "Description text",
                NoticeCategory.GENERAL,
                NoticePriority.NORMAL,
                NoticeStatus.PUBLISHED,
                null,
                false,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(2) // Expiry before publish!
        );

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/admin"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(invalidDates)))
                .build();
        HttpResponse<String> res1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, res1.statusCode(), "Expiry before publish date must return 400 Bad Request");

        // Non-existent target tower ID
        CreateNoticeRequest invalidTower = new CreateNoticeRequest(
                "Invalid Tower Notice",
                "Description text",
                NoticeCategory.GENERAL,
                NoticePriority.NORMAL,
                NoticeStatus.PUBLISHED,
                "T9999", // Non-existent tower!
                false,
                LocalDateTime.now(),
                null
        );

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/notices/admin"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(invalidTower)))
                .build();
        HttpResponse<String> res2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, res2.statusCode(), "Non-existent tower must return 400 Bad Request");
    }
}

