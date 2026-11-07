package com.township.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.township.dto.GenerateBillsRequest;
import com.township.dto.LoginRequest;
import com.township.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BillingControllerTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    private String neerajToken;
    private String helpDeskToken;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @BeforeEach
    void setupTokens() throws Exception {
        // 1. Login as Neeraj Kulkarni (Resident)
        LoginRequest residentReq = new LoginRequest("neeraj.kulkarni@grandvistapune.in", "Resident@123");
        String residentBody = objectMapper.writeValueAsString(residentReq);
        HttpRequest resHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(residentBody))
                .build();
        HttpResponse<String> resResponse = client.send(resHttp, HttpResponse.BodyHandlers.ofString());
        JsonNode resJson = objectMapper.readTree(resResponse.body());
        neerajToken = resJson.get("token").asText();

        // 2. Login as Col. Rajiv Sharma (Help Desk)
        LoginRequest hdReq = new LoginRequest("helpdesk@grandvistapune.in", "HelpDesk@123");
        String hdBody = objectMapper.writeValueAsString(hdReq);
        HttpRequest hdHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(hdBody))
                .build();
        HttpResponse<String> hdResponse = client.send(hdHttp, HttpResponse.BodyHandlers.ofString());
        JsonNode hdJson = objectMapper.readTree(hdResponse.body());
        helpDeskToken = hdJson.get("token").asText();
    }

    @Test
    @DisplayName("Resident can retrieve own bills with correct 6.50 base rate calculations")
    void testResidentMyBills() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/billing/my-bills"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode json = objectMapper.readTree(response.body());
        JsonNode bills = json.get("bills");
        assertNotNull(bills);
        assertTrue(bills.size() >= 1);

        // Find the October 2026 bill for T07-1203: 1150 sq ft * 6.50 = 7475.00
        JsonNode bill = null;
        for (JsonNode b : bills) {
            if ("2026-10".equals(b.get("billingMonth").asText())) {
                bill = b;
                break;
            }
        }
        assertNotNull(bill, "October 2026 bill for T07-1203 must be present");
        assertEquals("1203", bill.get("flatNumber").asText());
        assertEquals(1150.0, bill.get("flatAreaSqFt").asDouble());
        assertEquals(6.50, bill.get("ratePerSqFt").asDouble());
        assertEquals(7475.0, bill.get("baseAmount").asDouble());
        assertEquals(7475.0, bill.get("paidAmount").asDouble());
        assertEquals(0.0, bill.get("outstandingBalance").asDouble());
        assertEquals("PAID", bill.get("status").asText());
    }

    @Test
    @DisplayName("Help Desk can view all bills across township including overdue and partial payments")
    void testHelpDeskAllBillsOverview() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/billing/admin/bills"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode bills = objectMapper.readTree(response.body());
        assertTrue(bills.size() >= 3);

        // Verify Swati Joshi (T12-0408): 980 sq ft * 6.50 = 6370.00, paid 0, OVERDUE
        boolean foundSwati = false;
        // Verify Aamir Shaikh (T21-1601): 1640 sq ft * 6.50 = 10660.00, paid 5000, balance 5660, PARTIALLY_PAID
        boolean foundAamir = false;

        for (JsonNode b : bills) {
            if ("0408".equals(b.get("flatNumber").asText()) && "2026-09".equals(b.get("billingMonth").asText())) {
                assertEquals(980.0, b.get("flatAreaSqFt").asDouble());
                assertEquals(6370.0, b.get("baseAmount").asDouble());
                assertEquals(6370.0, b.get("paidAmount").asDouble() + b.get("outstandingBalance").asDouble(), 0.01);
                foundSwati = true;
            }
            if ("1601".equals(b.get("flatNumber").asText()) && "2026-10".equals(b.get("billingMonth").asText())) {
                assertEquals(1640.0, b.get("flatAreaSqFt").asDouble());
                assertEquals(10660.0, b.get("baseAmount").asDouble());
                assertTrue(b.get("paidAmount").asDouble() >= 5000.0);
                assertEquals(10660.0, b.get("paidAmount").asDouble() + b.get("outstandingBalance").asDouble(), 0.01);
                foundAamir = true;
            }
        }

        assertTrue(foundSwati, "Swati Joshi 980 sq ft bill must exist");
        assertTrue(foundAamir, "Aamir Shaikh 1640 sq ft bill must exist");
    }

    @Test
    @DisplayName("Resident data isolation: Neeraj cannot access other flats' ledger")
    void testResidentDataIsolation() throws Exception {
        // Flat 2 belongs to Swati Joshi (T12-0408)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/billing/ledger/2"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode(), "Resident must be forbidden from accessing other flat's ledger");
    }

    @Test
    @DisplayName("Resident data isolation: Neeraj CAN access his own flat's ledger (Flat 1)")
    void testResidentOwnLedgerAccess() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/billing/ledger/1"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode entries = objectMapper.readTree(response.body());
        assertTrue(entries.size() >= 2);
    }

    @Test
    @DisplayName("Unauthorized billing generation: Resident gets 403 Forbidden")
    void testUnauthorizedBillGeneration() throws Exception {
        GenerateBillsRequest genReq = new GenerateBillsRequest("2026-11", 15, "Unauthorized test run");
        String jsonBody = objectMapper.writeValueAsString(genReq);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/billing/admin/generate-monthly"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("Authorized Help Desk can generate monthly bills idempotently")
    void testAuthorizedIdempotentBillGeneration() throws Exception {
        int testYear = 2030 + (int) (Math.random() * 50);
        int testMonth = (int) (Math.random() * 12) + 1;
        String testCycle = String.format("%04d-%02d", testYear, testMonth);
        GenerateBillsRequest genReq = new GenerateBillsRequest(testCycle, 15, "Automated test cycle " + testCycle);
        String jsonBody = objectMapper.writeValueAsString(genReq);

        // First run: generates bills
        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/billing/admin/generate-monthly"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response1.statusCode());
        JsonNode json1 = objectMapper.readTree(response1.body());
        int generated = json1.get("billsGeneratedCount").asInt();
        assertTrue(generated > 0, "First generation run must generate bills");

        // Second run: duplicate prevention (idempotency)
        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/billing/admin/generate-monthly"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response2.statusCode());
        JsonNode json2 = objectMapper.readTree(response2.body());
        assertEquals(0, json2.get("billsGeneratedCount").asInt(), "Second run must not create duplicates");
        assertEquals(generated, json2.get("duplicatesSkippedCount").asInt(), "All existing bills must be skipped");
    }

    @Test
    @DisplayName("Public registration cannot create privileged roles; always creates RESIDENT")
    void testPublicRegistrationCannotCreatePrivilegedRole() throws Exception {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest registerReq = new RegisterRequest(
                "Attacker User",
                "attacker." + randomSuffix + "@test.in",
                "+91982299" + (int)(Math.random() * 8999 + 1000),
                "Secret@123",
                com.township.entity.Role.HELP_DESK, // Attempting to escalate role
                null,
                null
        );

        String jsonBody = objectMapper.writeValueAsString(registerReq);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());

        JsonNode userJson = objectMapper.readTree(response.body()).get("user");
        assertEquals("RESIDENT", userJson.get("role").asText(), "Public registration must force role to RESIDENT");
    }
}
