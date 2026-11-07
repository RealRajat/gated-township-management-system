package com.township.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.township.dto.*;
import com.township.entity.AmenityCategory;
import com.township.repository.AmenityBookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AmenityBookingControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AmenityBookingRepository bookingRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final HttpClient client = HttpClient.newHttpClient();

    private String neerajToken;
    private String aamirToken;
    private String swatiToken;
    private String helpDeskToken;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @BeforeEach
    void setupTokens() throws Exception {
        // Reset non-seeded test bookings to ensure pristine idempotency
        List<String> seededRefs = List.of("BK-782194", "BK-439120", "BK-519842", "BK-928371");
        bookingRepository.findAll().stream()
                .filter(b -> !seededRefs.contains(b.getBookingReference()))
                .forEach(bookingRepository::delete);

        // Resident 1: Neeraj Kulkarni (Flat 1, T07-1203)
        LoginRequest neerajLogin = new LoginRequest("neeraj.kulkarni@grandvistapune.in", "Resident@123");
        this.neerajToken = authenticateAndGetToken(neerajLogin);

        // Resident 2: Aamir Shaikh (Flat 3, T21-1601)
        LoginRequest aamirLogin = new LoginRequest("aamir.shaikh@grandvistapune.in", "Resident@123");
        this.aamirToken = authenticateAndGetToken(aamirLogin);

        // Resident 3: Swati Joshi (Flat 2, T12-0408)
        LoginRequest swatiLogin = new LoginRequest("swati.joshi@grandvistapune.in", "Resident@123");
        this.swatiToken = authenticateAndGetToken(swatiLogin);

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
        JsonNode root = objectMapper.readTree(res.body());
        return root.get("token").asText();
    }

    @Test
    @DisplayName("1. Catalog and Real-time Slot Availability Lookup")
    void testAmenityCatalogAndSlots() throws Exception {
        // GET /api/amenities
        HttpRequest listReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> listRes = client.send(listReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, listRes.statusCode());

        JsonNode amenities = objectMapper.readTree(listRes.body());
        assertTrue(amenities.isArray());
        assertTrue(amenities.size() >= 5, "Expected at least 5 seeded amenities");

        Long gymId = null;
        for (JsonNode a : amenities) {
            if ("GYM".equals(a.get("code").asText())) {
                gymId = a.get("id").asLong();
                assertEquals("FitLife Fitness Gym", a.get("name").asText());
                assertEquals(30, a.get("slotCapacity").asInt());
                assertEquals(60, a.get("slotDurationMinutes").asInt());
                assertTrue(a.get("isActive").asBoolean());
            }
        }
        assertNotNull(gymId, "Gym amenity should be present");

        // GET /api/amenities/{id}/slots for future date
        LocalDate targetDate = LocalDate.now().plusDays(4);
        HttpRequest slotReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities/" + gymId + "/slots?date=" + targetDate))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> slotRes = client.send(slotReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, slotRes.statusCode());

        JsonNode slots = objectMapper.readTree(slotRes.body());
        assertTrue(slots.isArray());
        assertTrue(slots.size() >= 10, "Should generate multiple hourly slots between 06:00 and 22:00");

        JsonNode firstSlot = slots.get(0);
        assertTrue(firstSlot.has("slotLabel"));
        assertTrue(firstSlot.has("totalCapacity"));
        assertTrue(firstSlot.has("availableSpots"));
        assertTrue(firstSlot.has("isAvailable"));
    }

    @Test
    @DisplayName("2. Resident Booking Creation and Cancellation Flow")
    void testResidentBookingCreationAndCancellation() throws Exception {
        // Lookup BADMINTON amenity id
        Long badmintonId = getAmenityIdByCode("BADMINTON");
        assertNotNull(badmintonId);

        LocalDate bookingDate = LocalDate.now().plusDays(5);
        LocalTime startTime = LocalTime.of(10, 0);

        CreateBookingRequest bookReq = new CreateBookingRequest(
                badmintonId,
                1L, // Flat T07-1203 (Neeraj's flat)
                bookingDate,
                startTime,
                2,
                "Weekend singles match"
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities/bookings"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(bookReq)))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, res.statusCode(), "Booking creation should succeed: " + res.body());

        JsonNode booked = objectMapper.readTree(res.body());
        Long bookingId = booked.get("id").asLong();
        String bookingRef = booked.get("bookingReference").asText();

        assertTrue(bookingRef.startsWith("BK-"));
        assertEquals("CONFIRMED", booked.get("status").asText());
        assertEquals("T07-1203", booked.get("flatNumber").asText());
        assertEquals(2, booked.get("attendeeCount").asInt());
        assertTrue(booked.get("canCancel").asBoolean());

        // Check in resident's My Bookings
        HttpRequest myBookingsReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities/my-bookings"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> myBookingsRes = client.send(myBookingsReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, myBookingsRes.statusCode());
        assertTrue(myBookingsRes.body().contains(bookingRef));

        // Lookup by reference code
        HttpRequest refReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities/bookings/" + bookingRef))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> refRes = client.send(refReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, refRes.statusCode());
        assertEquals(bookingRef, objectMapper.readTree(refRes.body()).get("bookingReference").asText());

        // Cancel the booking
        CancelBookingRequest cancelReq = new CancelBookingRequest("Change of travel plans");
        HttpRequest cancelHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities/bookings/" + bookingId + "/cancel"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(cancelReq)))
                .build();
        HttpResponse<String> cancelRes = client.send(cancelHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, cancelRes.statusCode(), "Cancellation should succeed: " + cancelRes.body());

        JsonNode cancelled = objectMapper.readTree(cancelRes.body());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals("Change of travel plans", cancelled.get("cancellationReason").asText());
        assertFalse(cancelled.get("canCancel").asBoolean());
    }

    @Test
    @DisplayName("3. Prevent Overlapping/Duplicate Bookings for Same Flat in Same Slot")
    void testPreventDuplicateBookingSameSlot() throws Exception {
        Long poolId = getAmenityIdByCode("POOL");
        assertNotNull(poolId);

        LocalDate date = LocalDate.now().plusDays(6);
        LocalTime time = LocalTime.of(7, 0);

        CreateBookingRequest req1 = new CreateBookingRequest(poolId, 1L, date, time, 1, "First booking");
        CreateBookingRequest req2 = new CreateBookingRequest(poolId, 1L, date, time, 1, "Duplicate booking");

        HttpResponse<String> res1 = sendPost("/amenities/bookings", neerajToken, req1);
        assertEquals(201, res1.statusCode(), "First booking must succeed");

        // Attempt second booking for the same flat and slot
        HttpResponse<String> res2 = sendPost("/amenities/bookings", neerajToken, req2);
        assertEquals(409, res2.statusCode(), "Duplicate booking must be rejected with 409 Conflict");
        assertTrue(res2.body().contains("already has a confirmed booking"));
    }

    @Test
    @DisplayName("4. Enforce Slot Capacity Limits")
    void testSlotCapacityLimit() throws Exception {
        Long badmintonId = getAmenityIdByCode("BADMINTON"); // Capacity is 4
        assertNotNull(badmintonId);

        LocalDate date = LocalDate.now().plusDays(7);
        LocalTime time = LocalTime.of(15, 0);

        // Aamir (Flat 3) books for 3 attendees
        CreateBookingRequest req1 = new CreateBookingRequest(badmintonId, 3L, date, time, 3, "Group match");
        HttpResponse<String> res1 = sendPost("/amenities/bookings", aamirToken, req1);
        assertEquals(201, res1.statusCode(), "Booking for 3 spots should succeed: " + res1.body());

        // Swati (Flat 2) attempts to book 2 attendees (Total would be 3 + 2 = 5 > 4)
        CreateBookingRequest req2 = new CreateBookingRequest(badmintonId, 2L, date, time, 2, "Another group");
        HttpResponse<String> res2 = sendPost("/amenities/bookings", swatiToken, req2);
        assertEquals(409, res2.statusCode(), "Exceeding capacity must return 409 Conflict");
        assertTrue(res2.body().contains("capacity exceeded"));

        // But booking for 1 attendee should fit the remaining capacity (3 + 1 = 4)
        CreateBookingRequest req3 = new CreateBookingRequest(badmintonId, 2L, date, time, 1, "Solo practice");
        HttpResponse<String> res3 = sendPost("/amenities/bookings", swatiToken, req3);
        assertEquals(201, res3.statusCode(), "Booking fitting remaining capacity should succeed");
    }

    @Test
    @DisplayName("5. Enforce Per-Flat Active Booking Limit")
    void testPerFlatActiveBookingLimit() throws Exception {
        Long hallId = getAmenityIdByCode("COMMUNITY_HALL"); // maxActiveBookingsPerFlat = 1
        assertNotNull(hallId);

        LocalDate date1 = LocalDate.now().plusDays(10);
        LocalDate date2 = LocalDate.now().plusDays(11);
        LocalTime time = LocalTime.of(13, 0);

        // First active booking for Flat 1 (Neeraj has 0 community hall bookings)
        CreateBookingRequest req1 = new CreateBookingRequest(hallId, 1L, date1, time, 1, "Family function");
        HttpResponse<String> res1 = sendPost("/amenities/bookings", neerajToken, req1);
        assertEquals(201, res1.statusCode(), "First booking should succeed: " + res1.body());

        // Second booking for Flat 1 while first is still active
        CreateBookingRequest req2 = new CreateBookingRequest(hallId, 1L, date2, time, 1, "Second event");
        HttpResponse<String> res2 = sendPost("/amenities/bookings", neerajToken, req2);
        assertEquals(409, res2.statusCode(), "Exceeding per-flat limit must return 409 Conflict");
        assertTrue(res2.body().contains("maximum allowed active bookings limit"));
    }

    @Test
    @DisplayName("6. Reject Past Date or Past Time Bookings")
    void testPastDateRejection() throws Exception {
        Long gymId = getAmenityIdByCode("GYM");
        assertNotNull(gymId);

        // Booking yesterday
        LocalDate yesterday = LocalDate.now().minusDays(1);
        CreateBookingRequest req = new CreateBookingRequest(gymId, 1L, yesterday, LocalTime.of(10, 0), 1, "Past booking");

        HttpResponse<String> res = sendPost("/amenities/bookings", neerajToken, req);
        assertEquals(400, res.statusCode(), "Past date booking must be rejected with 400 Bad Request");
        assertTrue(res.body().contains("past"));
    }

    @Test
    @DisplayName("7. Security: Resident Cannot Book for Other Flats")
    void testUnauthorizedFlatBooking() throws Exception {
        Long gymId = getAmenityIdByCode("GYM");
        assertNotNull(gymId);

        LocalDate date = LocalDate.now().plusDays(8);
        // Neeraj (Flat 1) attempts to book using Flat 3 (Aamir's flat)
        CreateBookingRequest fraudReq = new CreateBookingRequest(gymId, 3L, date, LocalTime.of(14, 0), 1, "Impersonation attempt");

        HttpResponse<String> res = sendPost("/amenities/bookings", neerajToken, fraudReq);
        assertEquals(403, res.statusCode(), "Booking for unauthorized flat must return 403 Forbidden");
    }

    @Test
    @DisplayName("8. Security: Resident Cannot Cancel Another Resident's Booking")
    void testCrossResidentCancellationForbidden() throws Exception {
        Long poolId = getAmenityIdByCode("POOL");
        assertNotNull(poolId);

        LocalDate date = LocalDate.now().plusDays(9);
        CreateBookingRequest aamirReq = new CreateBookingRequest(poolId, 3L, date, LocalTime.of(10, 0), 1, "Aamir swimming");
        HttpResponse<String> aamirRes = sendPost("/amenities/bookings", aamirToken, aamirReq);
        assertEquals(201, aamirRes.statusCode());
        Long bookingId = objectMapper.readTree(aamirRes.body()).get("id").asLong();

        // Neeraj attempts to cancel Aamir's booking
        CancelBookingRequest cancelReq = new CancelBookingRequest("Malicious cancel");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities/bookings/" + bookingId + "/cancel"))
                .header("Authorization", "Bearer " + neerajToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(cancelReq)))
                .build();
        HttpResponse<String> cancelRes = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, cancelRes.statusCode(), "Cross-flat cancellation must be rejected with 403 Forbidden");
    }

    @Test
    @DisplayName("9. Help Desk Administrative Management and RBAC")
    void testHelpDeskAdminOperations() throws Exception {
        // Help Desk Overview KPIs
        HttpRequest kpiReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities/admin/overview"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .GET()
                .build();
        HttpResponse<String> kpiRes = client.send(kpiReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, kpiRes.statusCode());
        JsonNode kpis = objectMapper.readTree(kpiRes.body());
        assertTrue(kpis.has("totalAmenities"));
        assertTrue(kpis.has("activeAmenities"));

        // Resident attempting to call admin overview must be blocked
        HttpRequest resAttempt = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities/admin/overview"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> resAttemptRes = client.send(resAttempt, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, resAttemptRes.statusCode(), "Resident must be forbidden from admin endpoints");

        // Help Desk creates a new amenity
        String uniqueCode = "SQUASH_" + System.currentTimeMillis() % 10000;
        CreateAmenityRequest createReq = new CreateAmenityRequest(
                "Squash Court " + uniqueCode,
                uniqueCode,
                AmenityCategory.SPORTS,
                "Glass-backed international squash court",
                "Sports Complex L2",
                LocalTime.of(7, 0),
                LocalTime.of(21, 0),
                45,
                2,
                2,
                2,
                "Non-marking shoes only"
        );

        HttpRequest createHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities/admin"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(createReq)))
                .build();
        HttpResponse<String> createRes = client.send(createHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createRes.statusCode(), "Help Desk creating amenity should return 201 Created: " + createRes.body());

        Long createdId = objectMapper.readTree(createRes.body()).get("id").asLong();

        // Toggle amenity status to inactive
        ToggleAmenityStatusRequest toggleReq = new ToggleAmenityStatusRequest(false, "Annual wooden flooring polish");
        HttpRequest toggleHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities/admin/" + createdId + "/toggle-status"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(toggleReq)))
                .build();
        HttpResponse<String> toggleRes = client.send(toggleHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, toggleRes.statusCode());
        assertFalse(objectMapper.readTree(toggleRes.body()).get("isActive").asBoolean());

        // Booking an inactive amenity must fail with 409 Conflict
        CreateBookingRequest bookInactiveReq = new CreateBookingRequest(
                createdId, 1L, LocalDate.now().plusDays(3), LocalTime.of(8, 30), 1, "Squash session"
        );
        HttpResponse<String> bookInactiveRes = sendPost("/amenities/bookings", neerajToken, bookInactiveReq);
        assertEquals(409, bookInactiveRes.statusCode(), "Booking inactive facility must fail with 409 Conflict");
        assertTrue(bookInactiveRes.body().contains("unavailable"));
    }

    @Test
    @DisplayName("10. Concurrent Booking Simulation & Race Condition Prevention")
    void testConcurrentBookingPrevention() throws Exception {
        // Create an exclusive single-capacity test amenity for concurrency verification
        String concCode = "VIP_LOUNGE_" + System.currentTimeMillis() % 10000;
        CreateAmenityRequest concAmenity = new CreateAmenityRequest(
                "VIP Lounge " + concCode,
                concCode,
                AmenityCategory.LEISURE,
                "Private single-party lounge",
                "Penthouse Suite",
                LocalTime.of(10, 0),
                LocalTime.of(20, 0),
                60,
                1, // Slot capacity strictly 1!
                5,
                1,
                "Quiet please"
        );

        HttpRequest createHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities/admin"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(concAmenity)))
                .build();
        HttpResponse<String> createRes = client.send(createHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createRes.statusCode());
        Long vipId = objectMapper.readTree(createRes.body()).get("id").asLong();

        LocalDate testDate = LocalDate.now().plusDays(15);
        LocalTime testTime = LocalTime.of(14, 0);

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // wait for simultaneous trigger

                    // Alternate between Neeraj (Flat 1) and Aamir (Flat 3)
                    String token = (idx % 2 == 0) ? neerajToken : aamirToken;
                    Long flatId = (idx % 2 == 0) ? 1L : 3L;

                    CreateBookingRequest req = new CreateBookingRequest(
                            vipId, flatId, testDate, testTime, 1, "Concurrent race test #" + idx
                    );

                    HttpResponse<String> res = sendPost("/amenities/bookings", token, req);
                    if (res.statusCode() == 201) {
                        successCount.incrementAndGet();
                    } else if (res.statusCode() == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // FIRE ALL THREADS SIMULTANEOUSLY

        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly ONE concurrent request must succeed in booking the capacity-1 slot!
        assertEquals(1, successCount.get(), "Under concurrent race condition, exactly 1 booking must succeed");
        assertEquals(threadCount - 1, conflictCount.get(), "All other concurrent attempts must receive 409 Conflict");
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Long getAmenityIdByCode(String code) throws Exception {
        HttpRequest listReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/amenities"))
                .header("Authorization", "Bearer " + neerajToken)
                .GET()
                .build();
        HttpResponse<String> listRes = client.send(listReq, HttpResponse.BodyHandlers.ofString());
        JsonNode amenities = objectMapper.readTree(listRes.body());
        for (JsonNode a : amenities) {
            if (code.equals(a.get("code").asText())) {
                return a.get("id").asLong();
            }
        }
        return null;
    }

    private HttpResponse<String> sendPost(String endpoint, String token, Object body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + endpoint))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
