package com.township.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.township.dto.AssignComplaintRequest;
import com.township.dto.CreateComplaintRequest;
import com.township.dto.LinkDuplicateRequest;
import com.township.dto.LoginRequest;
import com.township.dto.RateComplaintRequest;
import com.township.dto.UpdateComplaintStatusRequest;
import com.township.entity.ComplaintCategory;
import com.township.entity.ComplaintPriority;
import com.township.entity.ComplaintStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ComplaintControllerTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    private String neerajToken;
    private String aamirToken;
    private String helpDeskToken;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @BeforeEach
    void setupTokens() throws Exception {
        // Neeraj Kulkarni (Flat 1, T07-1203)
        LoginRequest neerajLogin = new LoginRequest("neeraj.kulkarni@grandvistapune.in", "Resident@123");
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(neerajLogin)))
                .build();
        HttpResponse<String> res1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
        this.neerajToken = objectMapper.readTree(res1.body()).get("token").asText();

        // Aamir Shaikh (Flat 3, T21-1601)
        LoginRequest aamirLogin = new LoginRequest("aamir.shaikh@grandvistapune.in", "Resident@123");
        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(aamirLogin)))
                .build();
        HttpResponse<String> res2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        this.aamirToken = objectMapper.readTree(res2.body()).get("token").asText();

        // Help Desk Staff
        LoginRequest helpDeskLogin = new LoginRequest("helpdesk@grandvistapune.in", "HelpDesk@123");
        HttpRequest req3 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(helpDeskLogin)))
                .build();
        HttpResponse<String> res3 = client.send(req3, HttpResponse.BodyHandlers.ofString());
        this.helpDeskToken = objectMapper.readTree(res3.body()).get("token").asText();
    }

    @Test
    @DisplayName("Resident creates complaint and access restriction protects data from other residents")
    void testResidentComplaintCreationAndAccessRestrictions() throws Exception {
        CreateComplaintRequest createReq = new CreateComplaintRequest(
                1L, // Flat 1 (Neeraj's flat)
                "Kitchen Sink Clogged",
                "Water is draining very slowly from the kitchen sink and backing up into the washbasin.",
                ComplaintCategory.PLUMBING,
                ComplaintPriority.MEDIUM
        );

        HttpRequest postHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(createReq)))
                .build();

        HttpResponse<String> postRes = client.send(postHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postRes.statusCode());
        JsonNode json = objectMapper.readTree(postRes.body());

        Long complaintId = json.get("id").asLong();
        assertTrue(json.get("complaintNumber").asText().startsWith("CMP-"));
        assertEquals("Kitchen Sink Clogged", json.get("title").asText());
        assertEquals("PLUMBING", json.get("category").asText());
        assertEquals("SUBMITTED", json.get("status").asText());
        assertNotNull(json.get("responseDeadline").asText());
        assertNotNull(json.get("resolutionDeadline").asText());

        // Neeraj can view his own complaint
        HttpRequest getHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + complaintId))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> getRes = client.send(getHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getRes.statusCode());

        // Aamir (different resident) is FORBIDDEN from viewing Neeraj's complaint
        HttpRequest unauthorizedHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + complaintId))
                .header("Authorization", "Bearer " + aamirToken)
                .GET()
                .build();
        HttpResponse<String> unauthorizedRes = client.send(unauthorizedHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, unauthorizedRes.statusCode(), "Resident must not access complaints of flats they do not belong to");
    }

    @Test
    @DisplayName("Critical safety keywords override AI and force CRITICAL priority")
    void testSafetyKeywordEscalationOverridingAi() throws Exception {
        // Description contains "gas leak" keyword
        CreateComplaintRequest safetyReq = new CreateComplaintRequest(
                1L,
                "Strong smell of LPG gas near utility riser",
                "Strong gas leak smell near the kitchen piped gas cylinder meter. High risk of hazard.",
                ComplaintCategory.OTHER, // Attempt to specify non-critical category
                ComplaintPriority.LOW     // Attempt to specify LOW priority
        );

        HttpRequest postHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(safetyReq)))
                .build();

        HttpResponse<String> postRes = client.send(postHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postRes.statusCode());
        JsonNode json = objectMapper.readTree(postRes.body());

        assertTrue(json.get("isCriticalSafetyEscalation").asBoolean(), "Must be flagged as critical safety escalation");
        assertEquals("CRITICAL", json.get("priority").asText(), "Priority must be unconditionally forced to CRITICAL");
        assertTrue(json.get("safetyTriggerKeyword").asText().toLowerCase().contains("gas leak")
                || json.get("safetyTriggerKeyword").asText().toLowerCase().contains("gas smell"));
    }

    @Test
    @DisplayName("AI Preview classification returns suggested category and detects safety triggers")
    void testAiPreviewClassification() throws Exception {
        // 1. Safety trigger preview
        Map<String, Object> previewSafety = Map.of(
                "title", "Corridor electrical issue",
                "description", "Exposed live wire sparking on the 12th floor lobby ceiling.",
                "flatId", 1L
        );

        HttpRequest previewHttp1 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/preview-classify"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(previewSafety)))
                .build();

        HttpResponse<String> res1 = client.send(previewHttp1, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res1.statusCode());
        JsonNode json1 = objectMapper.readTree(res1.body());
        assertTrue(json1.get("isSafetyEscalation").asBoolean());
        assertEquals("CRITICAL", json1.get("suggestedPriority").asText());
        assertEquals("SAFETY_RULE_ENGINE", json1.get("provider").asText());

        // 2. Normal plumbing preview
        Map<String, Object> previewPlumbing = Map.of(
                "title", "Bathroom tap issue",
                "description", "Water tap is dripping slowly in guest bathroom.",
                "flatId", 1L
        );

        HttpRequest previewHttp2 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/preview-classify"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(previewPlumbing)))
                .build();

        HttpResponse<String> res2 = client.send(previewHttp2, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res2.statusCode());
        JsonNode json2 = objectMapper.readTree(res2.body());
        assertFalse(json2.get("isSafetyEscalation").asBoolean());
        assertEquals("PLUMBING", json2.get("suggestedCategory").asText());
    }

    @Test
    @DisplayName("Help Desk workflow: assign team, update status to RESOLVED, and hide internal notes from residents")
    void testHelpDeskAssignmentStatusAndInternalNotes() throws Exception {
        // Neeraj files a complaint
        CreateComplaintRequest createReq = new CreateComplaintRequest(
                1L,
                "Intercom handset dead",
                "Intercom handset does not ring when main gate security calls.",
                ComplaintCategory.SECURITY,
                ComplaintPriority.MEDIUM
        );

        HttpRequest postHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(createReq)))
                .build();

        HttpResponse<String> postRes = client.send(postHttp, HttpResponse.BodyHandlers.ofString());
        Long complaintId = objectMapper.readTree(postRes.body()).get("id").asLong();

        // 1. Help Desk assigns complaint
        AssignComplaintRequest assignReq = new AssignComplaintRequest(
                "Apex Security Systems Cell",
                null,
                "Dispatched technician Suresh to inspect junction box."
        );

        HttpRequest assignHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + complaintId + "/assign"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(assignReq)))
                .build();

        HttpResponse<String> assignRes = client.send(assignHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, assignRes.statusCode());
        JsonNode assignJson = objectMapper.readTree(assignRes.body());
        assertEquals("ASSIGNED", assignJson.get("status").asText());
        assertEquals("Apex Security Systems Cell", assignJson.get("assignedTeam").asText());

        // 2. Help Desk adds an INTERNAL NOTE (Confidential to staff)
        Map<String, String> internalNoteReq = Map.of("internalNote", "Vendor quote approved up to INR 800.");
        HttpRequest noteHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + complaintId + "/internal-note"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(internalNoteReq)))
                .build();
        HttpResponse<String> noteRes = client.send(noteHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, noteRes.statusCode());

        // 3. Help Desk marks complaint as RESOLVED
        UpdateComplaintStatusRequest statusReq = new UpdateComplaintStatusRequest(
                ComplaintStatus.RESOLVED,
                "Replaced terminal connector in floor riser duct. Intercom fully tested.",
                "Completed within 2 hours."
        );

        HttpRequest statusHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + complaintId + "/status"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(statusReq)))
                .build();

        HttpResponse<String> statusRes = client.send(statusHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, statusRes.statusCode());
        JsonNode statusJson = objectMapper.readTree(statusRes.body());
        assertEquals("RESOLVED", statusJson.get("status").asText());
        assertNotNull(statusJson.get("resolvedAt"));

        // 4. Verify Resident views complaint: internal note must be HIDDEN
        HttpRequest residentViewHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + complaintId))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> residentViewRes = client.send(residentViewHttp, HttpResponse.BodyHandlers.ofString());
        JsonNode resJson = objectMapper.readTree(residentViewRes.body());
        for (JsonNode act : resJson.get("activities")) {
            assertTrue(act.get("internalNote").isNull(), "Resident MUST NOT see internal staff notes");
        }
    }

    @Test
    @DisplayName("Duplicate linking links complaints without closing or deleting duplicates")
    void testDuplicateDetectionAndLinking() throws Exception {
        // Complaint 1 (Master)
        CreateComplaintRequest c1 = new CreateComplaintRequest(1L, "Tower G Lobby Elevator Jerking", "Elevator jerks when reaching 5th floor.", ComplaintCategory.LIFT, ComplaintPriority.HIGH);
        HttpResponse<String> res1 = client.send(HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(c1)))
                .build(), HttpResponse.BodyHandlers.ofString());
        Long masterId = objectMapper.readTree(res1.body()).get("id").asLong();

        // Complaint 2 (Duplicate report)
        CreateComplaintRequest c2 = new CreateComplaintRequest(1L, "Tower G Lift heavy vibration", "Lift makes shuddering vibration on floor 5.", ComplaintCategory.LIFT, ComplaintPriority.HIGH);
        HttpResponse<String> res2 = client.send(HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(c2)))
                .build(), HttpResponse.BodyHandlers.ofString());
        Long duplicateId = objectMapper.readTree(res2.body()).get("id").asLong();

        // Help Desk links duplicate to master
        LinkDuplicateRequest linkReq = new LinkDuplicateRequest(masterId, "Same issue reported twice for Tower G passenger lift.");
        HttpRequest linkHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + duplicateId + "/link-duplicate"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(linkReq)))
                .build();

        HttpResponse<String> linkRes = client.send(linkHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, linkRes.statusCode());
        JsonNode linkJson = objectMapper.readTree(linkRes.body());

        assertEquals(masterId, linkJson.get("parentDuplicateOfId").asLong());
        assertNotEquals("CLOSED", linkJson.get("status").asText(), "Duplicate complaint must NOT be automatically closed or deleted");
    }

    @Test
    @DisplayName("Resident rating permissions: rating allowed only on resolved complaints by author")
    void testComplaintRatingPermissions() throws Exception {
        // File complaint
        CreateComplaintRequest createReq = new CreateComplaintRequest(
                1L,
                "Balcony drain choked",
                "Rainwater drain in balcony is clogged with dried leaves.",
                ComplaintCategory.PLUMBING,
                ComplaintPriority.LOW
        );

        HttpResponse<String> createRes = client.send(HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(createReq)))
                .build(), HttpResponse.BodyHandlers.ofString());
        Long complaintId = objectMapper.readTree(createRes.body()).get("id").asLong();

        RateComplaintRequest rateReq = new RateComplaintRequest(5, "Prompt resolution, very satisfied!");

        // 1. Attempt to rate when SUBMITTED -> Rejected
        HttpRequest earlyRate = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + complaintId + "/rate"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(rateReq)))
                .build();
        HttpResponse<String> earlyRateRes = client.send(earlyRate, HttpResponse.BodyHandlers.ofString());
        assertTrue(earlyRateRes.statusCode() >= 400, "Rating should not be allowed before resolution");

        // 2. Help Desk resolves complaint
        UpdateComplaintStatusRequest resolveReq = new UpdateComplaintStatusRequest(ComplaintStatus.RESOLVED, "Duct cleared.", null);
        client.send(HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + complaintId + "/status"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(resolveReq)))
                .build(), HttpResponse.BodyHandlers.ofString());

        // 3. Unauthorized resident attempts to rate -> Forbidden
        HttpRequest unauthRate = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + complaintId + "/rate"))
                .header("Authorization", "Bearer " + aamirToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(rateReq)))
                .build();
        assertEquals(403, client.send(unauthRate, HttpResponse.BodyHandlers.ofString()).statusCode());

        // 4. Authorized resident rates complaint -> Succeeded
        HttpResponse<String> rateSuccess = client.send(earlyRate, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, rateSuccess.statusCode());
        JsonNode ratedJson = objectMapper.readTree(rateSuccess.body());
        assertEquals(5, ratedJson.get("residentRating").asInt());
        assertEquals("Prompt resolution, very satisfied!", ratedJson.get("residentFeedback").asText());
    }

    @Test
    @DisplayName("Photo upload validation, safe storage and access control")
    void testPhotoUploadAndAccessControl() throws Exception {
        String boundary = "----Boundary" + UUID.randomUUID();
        byte[] fakeJpgBytes = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46 };

        StringBuilder part = new StringBuilder();
        part.append("--").append(boundary).append("\r\n");
        part.append("Content-Disposition: form-data; name=\"flatId\"\r\n\r\n1\r\n");
        part.append("--").append(boundary).append("\r\n");
        part.append("Content-Disposition: form-data; name=\"title\"\r\n\r\nWater Seepage on Ceiling\r\n");
        part.append("--").append(boundary).append("\r\n");
        part.append("Content-Disposition: form-data; name=\"description\"\r\n\r\nCeiling plaster peeling off due to seepage above.\r\n");
        part.append("--").append(boundary).append("\r\n");
        part.append("Content-Disposition: form-data; name=\"photo\"; filename=\"seepage.jpg\"\r\n");
        part.append("Content-Type: image/jpeg\r\n\r\n");

        byte[] headerBytes = part.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        byte[] fullPayload = new byte[headerBytes.length + fakeJpgBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, fullPayload, 0, headerBytes.length);
        System.arraycopy(fakeJpgBytes, 0, fullPayload, headerBytes.length, fakeJpgBytes.length);
        System.arraycopy(footerBytes, 0, fullPayload, headerBytes.length + fakeJpgBytes.length, footerBytes.length);

        HttpRequest uploadReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(fullPayload))
                .build();

        HttpResponse<String> uploadRes = client.send(uploadReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, uploadRes.statusCode());
        JsonNode json = objectMapper.readTree(uploadRes.body());

        Long complaintId = json.get("id").asLong();
        assertTrue(json.get("hasPhoto").asBoolean());
        assertNotNull(json.get("photoUrl").asText());

        // 1. Neeraj (author) can download photo
        HttpRequest photoGet = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + complaintId + "/photo"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<byte[]> photoRes = client.send(photoGet, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, photoRes.statusCode());
        assertEquals("image/jpeg", photoRes.headers().firstValue("Content-Type").orElse(""));

        // 2. Help Desk can access photo
        HttpRequest staffPhotoGet = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + complaintId + "/photo"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .GET()
                .build();
        assertEquals(200, client.send(staffPhotoGet, HttpResponse.BodyHandlers.ofByteArray()).statusCode());

        // 3. Unauthorized resident cannot access photo
        HttpRequest unauthPhotoGet = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/complaints/" + complaintId + "/photo"))
                .header("Authorization", "Bearer " + aamirToken)
                .GET()
                .build();
        assertEquals(403, client.send(unauthPhotoGet, HttpResponse.BodyHandlers.ofByteArray()).statusCode());
    }
}

