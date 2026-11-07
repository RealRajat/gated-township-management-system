package com.township.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.township.dto.*;
import com.township.entity.VisitorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VisitorControllerTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final HttpClient client = HttpClient.newHttpClient();

    private String neerajToken;
    private String aamirToken;
    private String guardToken;
    private String helpDeskToken;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @BeforeEach
    void setupTokens() throws Exception {
        // Resident 1: Neeraj Kulkarni (Flat 1, T07-1203)
        LoginRequest neerajLogin = new LoginRequest("neeraj.kulkarni@grandvistapune.in", "Resident@123");
        this.neerajToken = authenticateAndGetToken(neerajLogin);

        // Resident 2: Aamir Shaikh (Flat 3, T21-1601)
        LoginRequest aamirLogin = new LoginRequest("aamir.shaikh@grandvistapune.in", "Resident@123");
        this.aamirToken = authenticateAndGetToken(aamirLogin);

        // Guard: Santosh Shinde
        LoginRequest guardLogin = new LoginRequest("guard@grandvistapune.in", "Guard@123");
        this.guardToken = authenticateAndGetToken(guardLogin);

        // Help Desk: Col. Rajiv Sharma
        LoginRequest helpDeskLogin = new LoginRequest("helpdesk@grandvistapune.in", "HelpDesk@123");
        this.helpDeskToken = authenticateAndGetToken(helpDeskLogin);
    }

    private String authenticateAndGetToken(LoginRequest loginRequest) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(loginRequest)))
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Login should succeed: " + res.body());
        return objectMapper.readTree(res.body()).get("token").asText();
    }

    @Test
    @DisplayName("1. Resident pre-approves visitor and receives pass code + secure token")
    void testResidentPreApproveVisitor() throws Exception {
        PreApproveVisitorRequest request = new PreApproveVisitorRequest(
                1L, // Flat 1 (T07-1203)
                "Vikram Malhotra",
                "+919822099887",
                VisitorType.GUEST,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(12),
                "Family social dinner",
                "MH12-FA-9900",
                true
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/pre-approve"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, res.statusCode(), "Pre-approval should return 201 Created: " + res.body());

        JsonNode json = objectMapper.readTree(res.body());
        assertNotNull(json.get("id").asLong());
        assertTrue(json.get("passCode").asText().startsWith("VP-"));
        assertNotNull(json.get("passToken").asText());
        assertEquals("PRE_APPROVED", json.get("status").asText());
        assertEquals("Vikram Malhotra", json.get("visitorName").asText());
        assertEquals("T07", json.get("towerId").asText());
        assertEquals("1203", json.get("flatNumber").asText());
    }

    @Test
    @DisplayName("2. Resident cannot pre-approve visitor for a flat they do not belong to (403 Forbidden)")
    void testResidentCannotPreApproveForAnotherFlat() throws Exception {
        PreApproveVisitorRequest request = new PreApproveVisitorRequest(
                2L, // Flat 2 (T12-0408, belongs to Swati Joshi)
                "Intruder Guest",
                "+919822099999",
                VisitorType.GUEST,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(4),
                "Unauthorized entry attempt",
                null,
                true
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/pre-approve"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, res.statusCode(), "Expected 403 Forbidden for cross-flat pre-approval: " + res.body());
    }

    @Test
    @DisplayName("3. Resident revokes pass, preventing subsequent gate check-in")
    void testResidentRevokePass() throws Exception {
        // Step A: Pre-approve pass
        PreApproveVisitorRequest preApprove = new PreApproveVisitorRequest(
                1L,
                "Cancelled Guest",
                "+919822055667",
                VisitorType.GUEST,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(5),
                "Meeting cancelled",
                null,
                true
        );
        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/pre-approve"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(preApprove)))
                .build();
        HttpResponse<String> createRes = client.send(createReq, HttpResponse.BodyHandlers.ofString());
        Long visitorId = objectMapper.readTree(createRes.body()).get("id").asLong();
        String passCode = objectMapper.readTree(createRes.body()).get("passCode").asText();

        // Step B: Revoke pass as Neeraj
        HttpRequest revokeReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/" + visitorId + "/revoke"))
                .header("Authorization", "Bearer " + neerajToken)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> revokeRes = client.send(revokeReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, revokeRes.statusCode());
        assertEquals("REVOKED", objectMapper.readTree(revokeRes.body()).get("status").asText());

        // Step C: Guard tries to check in revoked pass -> must fail
        GateCheckInRequest checkInReq = new GateCheckInRequest(passCode, "Gate 1 (Main)", null);
        HttpRequest tryCheckIn = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/gate/check-in"))
                .header("Authorization", "Bearer " + guardToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(checkInReq)))
                .build();
        HttpResponse<String> tryRes = client.send(tryCheckIn, HttpResponse.BodyHandlers.ofString());
        assertNotEquals(200, tryRes.statusCode(), "Check-in of revoked pass must be denied");
    }

    @Test
    @DisplayName("4. Guard walk-in workflow, resident approval enforcement, entry & exit")
    void testGuardWalkInAndApprovalWorkflow() throws Exception {
        // Step A: Guard registers walk-in visitor for Neeraj's flat (Flat 1)
        WalkInVisitorRequest walkInReq = new WalkInVisitorRequest(
                1L,
                "Dr. Alok Sen",
                "+919822077654",
                VisitorType.SERVICE,
                "Home doctor consultation",
                "MH12-MD-1122",
                "Gate 1 (Main)"
        );
        HttpRequest walkInHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/gate/walk-in"))
                .header("Authorization", "Bearer " + guardToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(walkInReq)))
                .build();
        HttpResponse<String> walkInRes = client.send(walkInHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, walkInRes.statusCode());
        JsonNode walkInNode = objectMapper.readTree(walkInRes.body());
        Long visitorId = walkInNode.get("id").asLong();
        String passCode = walkInNode.get("passCode").asText();
        assertEquals("PENDING_APPROVAL", walkInNode.get("status").asText());

        // Step B: Guard attempts to approve visitor on resident's behalf -> MUST BE REJECTED (403)
        ResidentApprovalDecisionRequest approveDecision = new ResidentApprovalDecisionRequest(true, null);
        HttpRequest guardApproveTry = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/" + visitorId + "/decision"))
                .header("Authorization", "Bearer " + guardToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(approveDecision)))
                .build();
        HttpResponse<String> guardApproveRes = client.send(guardApproveTry, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, guardApproveRes.statusCode(), "Guard cannot approve visitors on behalf of resident");

        // Step C: Other resident (Aamir) tries to approve Neeraj's visitor -> 403 Forbidden
        HttpRequest aamirApproveTry = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/" + visitorId + "/decision"))
                .header("Authorization", "Bearer " + aamirToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(approveDecision)))
                .build();
        HttpResponse<String> aamirApproveRes = client.send(aamirApproveTry, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, aamirApproveRes.statusCode(), "Other resident cannot approve visitor for another flat");

        // Step D: Guard tries to check-in while still PENDING_APPROVAL -> Must fail
        GateCheckInRequest tryCheckIn = new GateCheckInRequest(passCode, "Gate 1 (Main)", null);
        HttpRequest prematureCheckIn = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/gate/check-in"))
                .header("Authorization", "Bearer " + guardToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(tryCheckIn)))
                .build();
        HttpResponse<String> prematureRes = client.send(prematureCheckIn, HttpResponse.BodyHandlers.ofString());
        assertNotEquals(200, prematureRes.statusCode(), "Cannot check-in unapproved visitor");

        // Step E: Resident Neeraj approves visitor
        HttpRequest residentApprove = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/" + visitorId + "/decision"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(approveDecision)))
                .build();
        HttpResponse<String> residentApproveRes = client.send(residentApprove, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, residentApproveRes.statusCode());
        assertEquals("APPROVED", objectMapper.readTree(residentApproveRes.body()).get("status").asText());

        // Step F: Guard checks in approved visitor
        HttpRequest validCheckIn = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/gate/check-in"))
                .header("Authorization", "Bearer " + guardToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(tryCheckIn)))
                .build();
        HttpResponse<String> checkInSuccessRes = client.send(validCheckIn, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, checkInSuccessRes.statusCode());
        assertEquals("CHECKED_IN", objectMapper.readTree(checkInSuccessRes.body()).get("status").asText());

        // Step G: Guard checks out visitor
        GateCheckOutRequest checkOutReq = new GateCheckOutRequest("Gate 2 (East)");
        HttpRequest validCheckOut = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/gate/check-out/" + visitorId))
                .header("Authorization", "Bearer " + guardToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(checkOutReq)))
                .build();
        HttpResponse<String> checkOutSuccessRes = client.send(validCheckOut, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, checkOutSuccessRes.statusCode());
        assertEquals("CHECKED_OUT", objectMapper.readTree(checkOutSuccessRes.body()).get("status").asText());
    }

    @Test
    @DisplayName("5. Pass lookup by QR token, pass code, and mobile phone")
    void testPassLookupMethods() throws Exception {
        // Pre-approve pass with specific phone
        String uniquePhone = "+9198220" + (10000 + (int)(Math.random() * 89999));
        PreApproveVisitorRequest preApprove = new PreApproveVisitorRequest(
                1L,
                "Courier Lookup Test",
                uniquePhone,
                VisitorType.DELIVERY,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(4),
                "Parcel lookup",
                null,
                true
        );
        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/pre-approve"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(preApprove)))
                .build();
        HttpResponse<String> createRes = client.send(createReq, HttpResponse.BodyHandlers.ofString());
        JsonNode json = objectMapper.readTree(createRes.body());
        String passCode = json.get("passCode").asText();
        String passToken = json.get("passToken").asText();

        // Guard searches by pass code
        HttpRequest searchByCode = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/gate/lookup?q=" + passCode))
                .header("Authorization", "Bearer " + guardToken)
                .GET().build();
        HttpResponse<String> res1 = client.send(searchByCode, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res1.statusCode());
        assertTrue(res1.body().contains(passCode));

        // Guard searches by QR token
        HttpRequest searchByToken = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/gate/lookup?q=" + passToken))
                .header("Authorization", "Bearer " + guardToken)
                .GET().build();
        HttpResponse<String> res2 = client.send(searchByToken, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res2.statusCode());
        assertTrue(res2.body().contains(passCode));

        // Guard searches by phone number
        HttpRequest searchByPhone = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/gate/lookup?q=" + URLEncoder.encode(uniquePhone, StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + guardToken)
                .GET().build();
        HttpResponse<String> res3 = client.send(searchByPhone, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res3.statusCode());
        assertTrue(res3.body().contains(passCode));
    }

    @Test
    @DisplayName("6. Offline synchronization idempotency: duplicate events skipped without error")
    void testOfflineSyncBatchAndIdempotency() throws Exception {
        // Step A: Pre-approve a pass
        PreApproveVisitorRequest preApprove = new PreApproveVisitorRequest(
                1L,
                "Offline Visitor Sync Test",
                "+919822066112",
                VisitorType.GUEST,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(4),
                "Offline check-in",
                "MH12-OF-9911",
                true
        );
        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/pre-approve"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(preApprove)))
                .build();
        HttpResponse<String> createRes = client.send(createReq, HttpResponse.BodyHandlers.ofString());
        String passCode = objectMapper.readTree(createRes.body()).get("passCode").asText();

        // Step B: Construct offline sync batch
        String syncEventId = UUID.randomUUID().toString();
        OfflineSyncItemDto checkInItem = new OfflineSyncItemDto(
                syncEventId,
                "CHECK_IN",
                passCode,
                LocalDateTime.now().minusMinutes(10),
                "Gate 1 (Offline Buffer)",
                "MH12-OF-9911"
        );
        OfflineSyncBatchRequest batchRequest = new OfflineSyncBatchRequest(List.of(checkInItem));

        // Step C: Send sync request 1st time
        HttpRequest syncReq1 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/gate/sync"))
                .header("Authorization", "Bearer " + guardToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(batchRequest)))
                .build();
        HttpResponse<String> syncRes1 = client.send(syncReq1, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, syncRes1.statusCode());
        JsonNode resJson1 = objectMapper.readTree(syncRes1.body());
        assertEquals(1, resJson1.get("successfullySynced").asInt());
        assertEquals(0, resJson1.get("duplicatesSkipped").asInt());

        // Step D: Re-send exact same batch request (duplicate simulation)
        HttpRequest syncReq2 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/gate/sync"))
                .header("Authorization", "Bearer " + guardToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(batchRequest)))
                .build();
        HttpResponse<String> syncRes2 = client.send(syncReq2, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, syncRes2.statusCode());
        JsonNode resJson2 = objectMapper.readTree(syncRes2.body());
        assertEquals(0, resJson2.get("successfullySynced").asInt(), "Duplicate event should not be re-processed");
        assertEquals(1, resJson2.get("duplicatesSkipped").asInt(), "Duplicate sync event should be cleanly skipped");
    }

    @Test
    @DisplayName("7. Help Desk triggers 90-day privacy retention cleanup endpoint")
    void test90DayPhotoRetentionCleanup() throws Exception {
        HttpRequest cleanupReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/visitors/admin/cleanup-photos"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> res = client.send(cleanupReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        JsonNode json = objectMapper.readTree(res.body());
        assertEquals("SUCCESS", json.get("status").asText());
        assertNotNull(json.get("purgedPhotoCount"));
    }
}
