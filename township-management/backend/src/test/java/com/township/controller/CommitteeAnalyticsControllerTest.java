package com.township.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.township.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommitteeAnalyticsControllerTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final HttpClient client = HttpClient.newHttpClient();

    private String committeeToken;
    private String helpDeskToken;
    private String residentToken;
    private String guardToken;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/committee";
    }

    @BeforeEach
    void setupTokens() throws Exception {
        this.committeeToken = authenticateAndGetToken("committee@grandvistapune.in", "Committee@123");
        this.helpDeskToken = authenticateAndGetToken("helpdesk@grandvistapune.in", "HelpDesk@123");
        this.residentToken = authenticateAndGetToken("neeraj.kulkarni@grandvistapune.in", "Resident@123");
        this.guardToken = authenticateAndGetToken("guard@grandvistapune.in", "Guard@123");
    }

    private String authenticateAndGetToken(String identifier, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(identifier, password);
        String body = objectMapper.writeValueAsString(loginRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Failed to authenticate test user: " + identifier);

        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.has("token"), "Response must contain JWT token");
        return root.get("token").asText();
    }

    // =========================================================================
    // 1. RBAC and Access Security Tests
    // =========================================================================
    @Test
    @DisplayName("ROLE_COMMITTEE can access committee dashboard")
    void committeeRoleCanAccessDashboard() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/dashboard"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.has("financial"));
        assertTrue(root.has("collectionTrends"));
        assertTrue(root.has("duesAgeing"));
        assertTrue(root.has("towerDues"));
        assertTrue(root.has("complaints"));
        assertTrue(root.has("operational"));
    }

    @Test
    @DisplayName("ROLE_HELP_DESK can also access committee dashboard")
    void helpDeskRoleCanAccessDashboard() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/dashboard"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    @DisplayName("ROLE_RESIDENT is strictly forbidden from accessing committee endpoints")
    void residentRoleForbiddenFromCommitteeDashboard() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/dashboard"))
                .header("Authorization", "Bearer " + residentToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("ROLE_GUARD is strictly forbidden from accessing committee endpoints")
    void guardRoleForbiddenFromCommitteeDashboard() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/dashboard"))
                .header("Authorization", "Bearer " + guardToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void unauthenticatedRequestReturns401() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/dashboard"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
    }

    // =========================================================================
    // 2. Financial KPIs & Collection Trends
    // =========================================================================
    @Test
    @DisplayName("GET /api/committee/financial returns accurate financial aggregates")
    void getFinancialKpisReturnsValidData() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/financial"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.has("totalBilled"));
        assertTrue(root.has("totalCollected"));
        assertTrue(root.has("totalOutstanding"));
        assertTrue(root.has("collectionPercentage"));
        assertTrue(root.get("totalBilled").asDouble() >= 0.0);
        assertTrue(root.get("collectionPercentage").asDouble() >= 0.0);
        assertTrue(root.get("collectionPercentage").asDouble() <= 100.0);
    }

    @Test
    @DisplayName("GET /api/committee/trends returns monthly collection trend history")
    void getTrendsReturnsMonthlyHistory() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/trends"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.isArray());
        if (root.size() > 0) {
            JsonNode firstTrend = root.get(0);
            assertTrue(firstTrend.has("month"));
            assertTrue(firstTrend.has("billedAmount"));
            assertTrue(firstTrend.has("collectedAmount"));
            assertTrue(firstTrend.has("collectionRate"));
        }
    }

    // =========================================================================
    // 3. Dues Ageing & Defaulter Directory (Privacy Verified)
    // =========================================================================
    @Test
    @DisplayName("GET /api/committee/dues-ageing returns four structured ageing buckets")
    void getDuesAgeingReturnsBuckets() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/dues-ageing"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.isArray());
        assertEquals(4, root.size());

        assertEquals("0–30 days", root.get(0).get("bucket").asText());
        assertEquals("31–60 days", root.get(1).get("bucket").asText());
        assertEquals("61–90 days", root.get(2).get("bucket").asText());
        assertEquals("Above 90 days", root.get(3).get("bucket").asText());
    }

    @Test
    @DisplayName("GET /api/committee/tower-dues returns tower-wise breakdown")
    void getTowerDuesReturnsSummaries() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/tower-dues"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.isArray());
        assertEquals(28, root.size(), "Should list all 28 seeded towers");
    }

    @Test
    @DisplayName("GET /api/committee/defaulters returns defaulters with privacy safety")
    void getDefaultersPreservesPrivacy() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/defaulters"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.isArray());

        // PRIVACY ENFORCEMENT VERIFICATION:
        // Defaulter summary must NOT contain personal resident phone or passwords
        for (JsonNode item : root) {
            assertFalse(item.has("phone"), "Defaulter DTO must not expose resident phone");
            assertFalse(item.has("password"), "Defaulter DTO must not expose passwords");
            assertTrue(item.has("flatNumber"));
            assertTrue(item.has("towerId"));
            assertTrue(item.has("outstandingDues"));
            assertTrue(item.has("daysOverdue"));
        }
    }

    // =========================================================================
    // 4. Complaint Analytics & Operational Metrics
    // =========================================================================
    @Test
    @DisplayName("GET /api/committee/complaints returns metrics, response times, and SLA breach")
    void getComplaintsReturnsAnalytics() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.has("totalComplaints"));
        assertTrue(root.has("openCount"));
        assertTrue(root.has("inProgressCount"));
        assertTrue(root.has("resolvedCount"));
        assertTrue(root.has("avgFirstResponseHours"));
        assertTrue(root.has("avgResolutionHours"));
        assertTrue(root.has("slaBreachCount"));
        assertTrue(root.has("categoryBreakdown"));
        assertTrue(root.has("priorityBreakdown"));
    }

    @Test
    @DisplayName("GET /api/committee/operational returns visitor, amenity, and notice metrics")
    void getOperationalReturnsMetrics() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/operational"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.has("totalVisitors"));
        assertTrue(root.has("totalAmenityBookings"));
        assertTrue(root.has("topAmenities"));
        assertTrue(root.has("totalNoticesPublished"));
        assertTrue(root.has("towerOperationalComparison"));
    }

    // =========================================================================
    // 5. CSV Report Exports
    // =========================================================================
    @Test
    @DisplayName("GET /api/committee/reports/financial/csv exports valid CSV")
    void exportFinancialCsvSucceeds() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/reports/financial/csv"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/csv"));
        assertTrue(response.headers().firstValue("Content-Disposition").orElse("").contains("attachment"));
        assertTrue(response.body().contains("Billing Month,Tower ID,Tower Name,Flat Number"));
    }

    @Test
    @DisplayName("GET /api/committee/reports/defaulters/csv exports valid CSV")
    void exportDefaultersCsvSucceeds() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/reports/defaulters/csv"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/csv"));
        assertTrue(response.body().contains("Tower ID,Tower Name,Flat Number,Carpet Area"));
    }

    @Test
    @DisplayName("GET /api/committee/reports/complaints/csv exports valid CSV")
    void exportComplaintsCsvSucceeds() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/reports/complaints/csv"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/csv"));
        assertTrue(response.body().contains("Complaint Number,Title,Tower,Flat Number"));
    }

    @Test
    @DisplayName("GET /api/committee/reports/operational/csv exports valid CSV")
    void exportOperationalCsvSucceeds() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/reports/operational/csv"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/csv"));
        assertTrue(response.body().contains("Tower ID,Tower Name,Total Visitors,Total Complaints"));
    }

    // =========================================================================
    // 6. Filtering Support
    // =========================================================================
    @Test
    @DisplayName("Dashboard supports tower and date-range filters")
    void dashboardFilterByTowerAndDate() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/dashboard?towerId=T07&fromDate=2026-01-01&toDate=2026-12-31"))
                .header("Authorization", "Bearer " + committeeToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode root = objectMapper.readTree(response.body());
        assertTrue(root.has("financial"));
    }
}

