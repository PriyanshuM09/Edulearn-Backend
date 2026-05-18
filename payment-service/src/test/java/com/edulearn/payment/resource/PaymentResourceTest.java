package com.edulearn.payment.resource;

import com.edulearn.payment.dto.*;
import com.edulearn.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentResource.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/payments/create-order - Success")
    void createOrder_Success() throws Exception {
        // given
        PaymentRequestDto request = new PaymentRequestDto();
        request.setStudentId(1);
        request.setCourseId(101);
        request.setAmount(1000.0);

        PaymentResponseDto response = new PaymentResponseDto();
        response.setRazorpayOrderId("order_123");

        when(paymentService.createOrder(any(PaymentRequestDto.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_123"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/student/{studentId} - Success")
    void getPaymentsByStudent_Success() throws Exception {
        // given
        PaymentResponseDto response = new PaymentResponseDto();
        response.setRazorpayOrderId("order_123");

        when(paymentService.getPaymentsByStudent(anyInt())).thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/payments/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].razorpayOrderId").value("order_123"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/verify - Success")
    void verifyPayment_Success() throws Exception {
        PaymentVerifyDto request = new PaymentVerifyDto();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("sig");
        when(paymentService.verifyPayment(any(PaymentVerifyDto.class))).thenReturn(paymentResponse());

        mockMvc.perform(post("/api/v1/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_123"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/{paymentId}/refund - Success")
    void refundPayment_Success() throws Exception {
        when(paymentService.refundPayment(1)).thenReturn(paymentResponse());

        mockMvc.perform(post("/api/v1/payments/1/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_123"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/{paymentId} - Success")
    void getPaymentById_Success() throws Exception {
        when(paymentService.getPaymentById(1)).thenReturn(paymentResponse());

        mockMvc.perform(get("/api/v1/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_123"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/course/{courseId} - Success")
    void getByCourse_Success() throws Exception {
        when(paymentService.getPaymentsByCourse(101)).thenReturn(List.of(paymentResponse()));

        mockMvc.perform(get("/api/v1/payments/course/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].razorpayOrderId").value("order_123"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/status/{status} - Success")
    void getByStatus_Success() throws Exception {
        when(paymentService.getPaymentsByStatus("SUCCESS")).thenReturn(List.of(paymentResponse()));

        mockMvc.perform(get("/api/v1/payments/status/SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].razorpayOrderId").value("order_123"));
    }

    @Test
    @DisplayName("Subscription endpoints - Success")
    void subscriptionEndpoints_Success() throws Exception {
        SubscriptionRequestDto request = new SubscriptionRequestDto();
        request.setStudentId(1);
        request.setPlanType("MONTHLY");
        request.setAmountPaid(100.0);
        SubscriptionResponseDto response = new SubscriptionResponseDto();
        response.setSubscriptionId(1);

        when(paymentService.createSubscription(any(SubscriptionRequestDto.class))).thenReturn(response);
        when(paymentService.getSubscriptionById(1)).thenReturn(response);
        when(paymentService.getSubscriptionsByStudent(1)).thenReturn(List.of(response));
        when(paymentService.cancelSubscription(1)).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriptionId").value(1));
        mockMvc.perform(get("/api/v1/payments/subscriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(1));
        mockMvc.perform(get("/api/v1/payments/subscriptions/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subscriptionId").value(1));
        mockMvc.perform(put("/api/v1/payments/subscriptions/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(1));
    }

    @Test
    @DisplayName("Wallet endpoints - Success")
    void walletEndpoints_Success() throws Exception {
        WalletResponseDto wallet = WalletResponseDto.builder()
                .studentId(1)
                .balance(200.0)
                .transactions(List.of())
                .build();
        PaymentVerifyDto verifyDto = new PaymentVerifyDto();
        verifyDto.setRazorpayOrderId("order_wallet");
        verifyDto.setRazorpayPaymentId("pay_1");
        verifyDto.setRazorpaySignature("sig");

        when(paymentService.getWallet(1)).thenReturn(wallet);
        when(paymentService.addFundsToWallet(any(PaymentRequestDto.class))).thenReturn(paymentResponse());
        when(paymentService.verifyWalletFunds(any(PaymentVerifyDto.class))).thenReturn(wallet);
        when(paymentService.payWithWallet(anyInt(), anyInt(), anyDouble())).thenReturn(paymentResponse());
        when(paymentService.adminRefundToWallet(anyInt(), anyInt(), anyDouble())).thenReturn(paymentResponse());

        mockMvc.perform(get("/api/v1/payments/wallet/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(200.0));
        mockMvc.perform(post("/api/v1/payments/wallet/add-funds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/payments/wallet/verify-funds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(200.0));
        mockMvc.perform(post("/api/v1/payments/wallet/pay")
                        .param("studentId", "1")
                        .param("courseId", "101")
                        .param("amount", "50.0"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/payments/wallet/admin/refund")
                        .param("studentId", "1")
                        .param("courseId", "101")
                        .param("amount", "50.0"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/payments/debug/reset-wallet/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Wallet for student 1 reset to 0"));
    }

    @Test
    @DisplayName("Refund request endpoints - Success")
    void refundRequestEndpoints_Success() throws Exception {
        RefundRequestDto refund = RefundRequestDto.builder()
                .id(1)
                .studentId(1)
                .courseId(101)
                .amount(100.0)
                .status("PENDING")
                .build();
        when(paymentService.createRefundRequest(any(RefundRequestDto.class))).thenReturn(refund);
        when(paymentService.getAllRefundRequests()).thenReturn(List.of(refund));
        when(paymentService.processRefundRequest(1, "APPROVED", 75.0)).thenReturn(refund);

        mockMvc.perform(post("/api/v1/payments/refund-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refund)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(get("/api/v1/payments/refund-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        mockMvc.perform(post("/api/v1/payments/refund-requests/1/process")
                        .param("status", "APPROVED")
                        .param("refundAmount", "75.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    private PaymentRequestDto paymentRequest() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setStudentId(1);
        request.setCourseId(101);
        request.setAmount(1000.0);
        return request;
    }

    private PaymentResponseDto paymentResponse() {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setRazorpayOrderId("order_123");
        response.setStatus("SUCCESS");
        return response;
    }
}
