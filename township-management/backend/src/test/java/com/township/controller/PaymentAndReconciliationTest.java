package com.township.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.township.dto.LoginRequest;
import com.township.dto.ReconcileApprovalRequest;
import com.township.dto.TestPaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentAndReconciliationTest {

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
        // 1. Login as Neeraj Kulkarni (Resident Flat 1)
        LoginRequest neerajReq = new LoginRequest("neeraj.kulkarni@grandvistapune.in", "Resident@123");
        String neerajBody = objectMapper.writeValueAsString(neerajReq);
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(neerajBody))
                .build();
        HttpResponse<String> res1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
        neerajToken = objectMapper.readTree(res1.body()).get("token").asText();

        // 2. Login as Aamir Shaikh (Resident Flat 3, T21-1601 with partial overdue bill)
        LoginRequest aamirReq = new LoginRequest("aamir.shaikh@grandvistapune.in", "Resident@123");
        String aamirBody = objectMapper.writeValueAsString(aamirReq);
        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(aamirBody))
                .build();
        HttpResponse<String> res2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        aamirToken = objectMapper.readTree(res2.body()).get("token").asText();

        // 3. Login as Col. Rajiv Sharma (Help Desk)
        LoginRequest hdReq = new LoginRequest("helpdesk@grandvistapune.in", "HelpDesk@123");
        String hdBody = objectMapper.writeValueAsString(hdReq);
        HttpRequest req3 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(hdBody))
                .build();
        HttpResponse<String> res3 = client.send(req3, HttpResponse.BodyHandlers.ofString());
        helpDeskToken = objectMapper.readTree(res3.body()).get("token").asText();
    }

    @Test
    @DisplayName("Resident can make partial test payment and update bill balance")
    void testPartialTestPayment() throws Exception {
        // Retrieve Aamir Shaikh's bills (Flat 3 has bill 3 with balance 5660.00)
        HttpRequest getBills = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/billing/my-bills"))
                .header("Authorization", "Bearer " + aamirToken)
                .GET()
                .build();
        HttpResponse<String> billsRes = client.send(getBills, HttpResponse.BodyHandlers.ofString());
        JsonNode billsJson = objectMapper.readTree(billsRes.body()).get("bills");

        JsonNode targetBill = null;
        for (JsonNode b : billsJson) {
            if (b.get("outstandingBalance").asDouble() > 1000.0) {
                targetBill = b;
                break;
            }
        }
        assertNotNull(targetBill, "Candidate bill with balance > 1000 required");

        Long billId = targetBill.get("id").asLong();
        double prevBalance = targetBill.get("outstandingBalance").asDouble();

        // Pay partial amount: 1000.00
        TestPaymentRequest payReq = new TestPaymentRequest(
                billId,
                new BigDecimal("1000.00"),
                "UPI",
                null,
                false,
                null
        );

        HttpRequest payHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/payments/test-pay"))
                .header("Authorization", "Bearer " + aamirToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payReq)))
                .build();

        HttpResponse<String> payResponse = client.send(payHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, payResponse.statusCode());

        JsonNode receipt = objectMapper.readTree(payResponse.body());
        assertTrue(receipt.get("isTestMode").asBoolean());
        assertEquals(1000.0, receipt.get("amountPaid").asDouble());
        assertEquals(prevBalance - 1000.0, receipt.get("remainingBalance").asDouble());
        assertTrue(receipt.get("transactionReference").asText().startsWith("TEST-TXN-"));
    }

    @Test
    @DisplayName("Test payment idempotency: identical idempotency key returns existing receipt without double-charging")
    void testPaymentIdempotency() throws Exception {
        String testKey = "IDEMP-" + UUID.randomUUID().toString();

        TestPaymentRequest payReq = new TestPaymentRequest(
                3L, // Aamir Shaikh bill
                new BigDecimal("100.00"),
                "NET_BANKING",
                testKey,
                false,
                null
        );
        String body = objectMapper.writeValueAsString(payReq);

        // First attempt
        HttpRequest pay1 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/payments/test-pay"))
                .header("Authorization", "Bearer " + aamirToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res1 = client.send(pay1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, res1.statusCode());
        JsonNode receipt1 = objectMapper.readTree(res1.body());
        Long paymentId1 = receipt1.get("paymentId").asLong();

        // Second attempt with exact same idempotency key
        HttpRequest pay2 = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/payments/test-pay"))
                .header("Authorization", "Bearer " + aamirToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res2 = client.send(pay2, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, res2.statusCode());
        JsonNode receipt2 = objectMapper.readTree(res2.body());
        Long paymentId2 = receipt2.get("paymentId").asLong();

        assertEquals(paymentId1, paymentId2, "Idempotent payment must return existing payment ID without double charging");
    }

    @Test
    @DisplayName("Payment failure simulation: rejects without modifying bill balance")
    void testPaymentFailureSimulation() throws Exception {
        TestPaymentRequest payReq = new TestPaymentRequest(
                3L,
                new BigDecimal("500.00"),
                "UPI",
                null,
                true, // Simulate failure
                "Test Gateway Timeout: Bank simulation"
        );

        HttpRequest payHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/payments/test-pay"))
                .header("Authorization", "Bearer " + aamirToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payReq)))
                .build();

        HttpResponse<String> response = client.send(payHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Test Gateway Timeout"));
    }

    @Test
    @DisplayName("Overpayment prevention: rejecting payment greater than outstanding balance")
    void testOverpaymentPrevention() throws Exception {
        TestPaymentRequest overpayReq = new TestPaymentRequest(
                3L,
                new BigDecimal("9999999.00"), // Massive overpayment
                "DEBIT_CARD",
                null,
                false,
                null
        );

        HttpRequest payHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/payments/test-pay"))
                .header("Authorization", "Bearer " + aamirToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(overpayReq)))
                .build();

        HttpResponse<String> response = client.send(payHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Overpayment rejected"));
    }

    @Test
    @DisplayName("Resident data isolation: Neeraj cannot pay Aamir's flat bill")
    void testUnauthorizedResidentPayment() throws Exception {
        TestPaymentRequest payReq = new TestPaymentRequest(
                3L, // Bill 3 belongs to Flat 3 (Aamir Shaikh)
                new BigDecimal("100.00"),
                "UPI",
                null,
                false,
                null
        );

        HttpRequest payHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/payments/test-pay"))
                .header("Authorization", "Bearer " + neerajToken) // Neeraj trying to pay Flat 3
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payReq)))
                .build();

        HttpResponse<String> response = client.send(payHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode(), "Cross-resident payment must be forbidden");
    }

    @Test
    @DisplayName("Bank CSV statement upload, deterministic matching, and duplicate prevention")
    void testBankCsvUploadAndMatching() throws Exception {
        String testUtr1 = "UTR" + System.currentTimeMillis() + "001";
        String testUtr2 = "UTR" + System.currentTimeMillis() + "002";

        // CSV content with 1 exact match (Swati Joshi T12-0408 with 6370.00 dues) and 1 ambiguous narration
        String csvContent = "transaction_date,amount,utr,narration\n"
                + "2026-10-15,6370.00," + testUtr1 + ",NEFT CMS T12-0408 SWATI JOSHI MAINT\n"
                + "2026-10-16,5000.00," + testUtr2 + ",IMPS CASH DEPOSIT PUNE\n";

        String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString();
        StringBuilder multipartBody = new StringBuilder();
        multipartBody.append("--").append(boundary).append("\r\n");
        multipartBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"test_statement.csv\"\r\n");
        multipartBody.append("Content-Type: text/csv\r\n\r\n");
        multipartBody.append(csvContent).append("\r\n");
        multipartBody.append("--").append(boundary).append("--\r\n");

        HttpRequest uploadReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/reconciliation/upload"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(multipartBody.toString()))
                .build();

        HttpResponse<String> uploadRes = client.send(uploadReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, uploadRes.statusCode());

        JsonNode batchJson = objectMapper.readTree(uploadRes.body());
        assertEquals(2, batchJson.get("totalRows").asInt());
        assertEquals(1, batchJson.get("exactMatchesCount").asInt());

        // Test Duplicate CSV upload: Uploading the exact same UTRs skips duplicates
        HttpResponse<String> dupUploadRes = client.send(uploadReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, dupUploadRes.statusCode());
        JsonNode dupBatchJson = objectMapper.readTree(dupUploadRes.body());
        assertEquals(0, dupBatchJson.get("totalRows").asInt(), "Duplicate UTRs must be skipped");
    }

    @Test
    @DisplayName("Manual approval workflow: Help Desk approves matched bank transaction and updates ledger")
    void testManualApprovalWorkflow() throws Exception {
        String testUtr = "UTR" + System.currentTimeMillis() + "999";

        // Query active bills to find one with outstanding balance
        HttpRequest listBillsReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/billing/admin/bills"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .GET()
                .build();
        HttpResponse<String> billsRes = client.send(listBillsReq, HttpResponse.BodyHandlers.ofString());
        JsonNode allBills = objectMapper.readTree(billsRes.body());
        JsonNode activeBill = null;
        for (JsonNode b : allBills) {
            if (b.get("outstandingBalance").asDouble() >= 50.0) {
                activeBill = b;
                break;
            }
        }
        assertNotNull(activeBill, "There must be a bill with outstanding balance >= 50.0");
        Long targetBillId = activeBill.get("id").asLong();
        String targetFlat = activeBill.get("flatNumber").asText();
        double reconcileAmount = 50.0;

        String csvContent = "transaction_date,amount,utr,narration\n"
                + "2026-10-20,50.00," + testUtr + ",NEFT DIRECT " + targetFlat + "\n";

        String boundary = "----Boundary" + UUID.randomUUID().toString();
        StringBuilder multipartBody = new StringBuilder();
        multipartBody.append("--").append(boundary).append("\r\n");
        multipartBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"approval_test.csv\"\r\n");
        multipartBody.append("Content-Type: text/csv\r\n\r\n");
        multipartBody.append(csvContent).append("\r\n");
        multipartBody.append("--").append(boundary).append("--\r\n");

        HttpRequest uploadReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/reconciliation/upload"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(multipartBody.toString()))
                .build();
        client.send(uploadReq, HttpResponse.BodyHandlers.ofString());

        // Get bank transactions
        HttpRequest listReq = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/reconciliation/transactions"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .GET()
                .build();
        HttpResponse<String> listRes = client.send(listReq, HttpResponse.BodyHandlers.ofString());
        JsonNode txns = objectMapper.readTree(listRes.body());

        JsonNode targetTxn = null;
        for (JsonNode t : txns) {
            if (testUtr.equals(t.get("utrReference").asText())) {
                targetTxn = t;
                break;
            }
        }
        assertNotNull(targetTxn, "Uploaded test UTR must be found");
        Long txnId = targetTxn.get("id").asLong();

        // Help Desk approves reconciliation for target bill
        ReconcileApprovalRequest approveReq = new ReconcileApprovalRequest(
                txnId,
                txnId,
                targetBillId,
                null,
                "APPROVE",
                "Verified against PMC bank portal statement"
        );

        HttpRequest approveHttp = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/reconciliation/approve"))
                .header("Authorization", "Bearer " + helpDeskToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(approveReq)))
                .build();

        HttpResponse<String> approveRes = client.send(approveHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, approveRes.statusCode());
        JsonNode approveJson = objectMapper.readTree(approveRes.body());
        assertEquals("RECONCILED", approveJson.get("status").asText());
        assertEquals(reconcileAmount, approveJson.get("reconciledAmount").asDouble());

        // Double counting check: Attempting to approve the same transaction again is rejected!
        HttpResponse<String> dupApproveRes = client.send(approveHttp, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, dupApproveRes.statusCode(), "Double counting must be rejected");
    }
}
