package com.township.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.township.dto.LoginRequest;
import com.township.dto.RegisterRequest;
import com.township.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient client = HttpClient.newHttpClient();

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @Test
    @DisplayName("POST /api/auth/login with valid seeded resident credentials returns 200 and JWT")
    void testValidLogin() throws Exception {
        LoginRequest loginReq = new LoginRequest("neeraj.kulkarni@grandvistapune.in", "Resident@123");
        String jsonBody = objectMapper.writeValueAsString(loginReq);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Map responseMap = objectMapper.readValue(response.body(), Map.class);
        assertTrue(responseMap.containsKey("token"));
        assertEquals("Bearer", responseMap.get("tokenType"));

        Map userMap = (Map) responseMap.get("user");
        assertNotNull(userMap);
        assertEquals("Neeraj Kulkarni", userMap.get("fullName"));
        assertEquals("RESIDENT", userMap.get("role"));
    }

    @Test
    @DisplayName("POST /api/auth/login with valid seeded phone number returns 200 and JWT")
    void testLoginWithPhone() throws Exception {
        LoginRequest loginReq = new LoginRequest("+919822012031", "Resident@123");
        String jsonBody = objectMapper.writeValueAsString(loginReq);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Map responseMap = objectMapper.readValue(response.body(), Map.class);
        assertTrue(responseMap.containsKey("token"));
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid password returns 401 Unauthorized")
    void testInvalidPasswordLogin() throws Exception {
        LoginRequest loginReq = new LoginRequest("neeraj.kulkarni@grandvistapune.in", "WrongPassword999");
        String jsonBody = objectMapper.writeValueAsString(loginReq);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        Map responseMap = objectMapper.readValue(response.body(), Map.class);
        assertEquals(401, responseMap.get("status"));
    }

    @Test
    @DisplayName("POST /api/auth/register with new resident details creates user and returns 201")
    void testRegisterNewResident() throws Exception {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis()).substring(7);
        RegisterRequest registerReq = new RegisterRequest(
                "Rohan Mehta",
                "rohan.mehta." + uniqueSuffix + "@pune.in",
                "+919822" + uniqueSuffix,
                "Rohan@123",
                Role.RESIDENT,
                "T07",
                "1402"
        );
        String jsonBody = objectMapper.writeValueAsString(registerReq);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        Map responseMap = objectMapper.readValue(response.body(), Map.class);
        assertTrue(responseMap.containsKey("token"));
        Map userMap = (Map) responseMap.get("user");
        assertEquals("Rohan Mehta", userMap.get("fullName"));
    }

    @Test
    @DisplayName("GET /api/auth/me without token returns 401 Unauthorized")
    void testUnauthorizedAccessToMe() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/me"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    @Test
    @DisplayName("GET /api/auth/me with valid Bearer token returns 200 and user profile")
    void testAuthorizedAccessToMe() throws Exception {
        // Step 1: Login to get token
        LoginRequest loginReq = new LoginRequest("neeraj.kulkarni@grandvistapune.in", "Resident@123");
        String loginJson = objectMapper.writeValueAsString(loginReq);

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                .build();

        HttpResponse<String> loginResp = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginResp.statusCode());
        Map loginMap = objectMapper.readValue(loginResp.body(), Map.class);
        String token = (String) loginMap.get("token");

        // Step 2: Request /me with Bearer token
        HttpRequest meRequest = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/me"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> meResp = client.send(meRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, meResp.statusCode());
        Map userProfile = objectMapper.readValue(meResp.body(), Map.class);
        assertEquals("neeraj.kulkarni@grandvistapune.in", userProfile.get("email"));
        assertEquals("RESIDENT", userProfile.get("role"));
    }
}
