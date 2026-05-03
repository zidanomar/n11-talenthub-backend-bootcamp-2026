package com.example.payment.controller;

import com.example.payment.dto.InitiatePaymentResponse;
import com.example.payment.dto.VerifyPaymentResult;
import com.example.payment.provider.PaymentMethod;
import com.example.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(RestExceptionHandler.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "payment.frontend-order-url=http://localhost:3000/orders"
})
@DisplayName("PaymentController")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private static final String VALID_BUYER = """
            {"id":"u1","name":"John","surname":"Doe",
             "email":"j@e.com","phone":"+9050000","identityNumber":"11111111111",
             "addressLine":"a","city":"c","country":"TR","zipCode":"06000"}
            """;

    private static final String VALID_INITIATE_BODY = """
            {
              "orderId": 1,
              "method": "IYZICO",
              "totalPrice": 100.00,
              "buyer": %s,
              "items": [{"id":"1","name":"P","category":"cat","price":100.00}]
            }
            """.formatted(VALID_BUYER);

    // ─────────────────────────── POST /api/payments/initiate ──────────

    @Nested
    @DisplayName("POST /api/payments/initiate")
    class Initiate {

        @Test
        @DisplayName("valid request → 200 with token + url")
        void ok() throws Exception {
            when(paymentService.initiate(any()))
                    .thenReturn(new InitiatePaymentResponse(1L, PaymentMethod.IYZICO, "tok", "https://pay/"));

            mockMvc.perform(post("/api/payments/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_INITIATE_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("tok"))
                    .andExpect(jsonPath("$.paymentPageUrl").value("https://pay/"));
        }

        @Test
        @DisplayName("missing orderId → 400")
        void missingOrderId() throws Exception {
            String body = """
                    {"method":"IYZICO","totalPrice":100.00,"buyer":%s,
                     "items":[{"id":"1","name":"P","category":"c","price":1.00}]}
                    """.formatted(VALID_BUYER);

            mockMvc.perform(post("/api/payments/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field == 'orderId')]").exists());
        }

        @Test
        @DisplayName("non-positive totalPrice → 400")
        void zeroTotal() throws Exception {
            String body = """
                    {"orderId":1,"method":"IYZICO","totalPrice":0,"buyer":%s,
                     "items":[{"id":"1","name":"P","category":"c","price":1.00}]}
                    """.formatted(VALID_BUYER);

            mockMvc.perform(post("/api/payments/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field == 'totalPrice')]").exists());
        }

        @Test
        @DisplayName("empty items → 400")
        void emptyItems() throws Exception {
            String body = """
                    {"orderId":1,"method":"IYZICO","totalPrice":100.00,"buyer":%s,
                     "items":[]}
                    """.formatted(VALID_BUYER);

            mockMvc.perform(post("/api/payments/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[?(@.field == 'items')]").exists());
        }

        @Test
        @DisplayName("buyer with blank fields → 400 with nested field errors")
        void blankBuyerFields() throws Exception {
            String body = """
                    {"orderId":1,"method":"IYZICO","totalPrice":100.00,
                     "buyer":{"id":"","name":"","surname":"","email":"","phone":"",
                              "identityNumber":"","addressLine":"","city":"","country":"","zipCode":""},
                     "items":[{"id":"1","name":"P","category":"c","price":1.00}]}
                    """;

            mockMvc.perform(post("/api/payments/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("invalid method enum → 400")
        void invalidMethod() throws Exception {
            String body = """
                    {"orderId":1,"method":"PAYPAL","totalPrice":100.00,"buyer":%s,
                     "items":[{"id":"1","name":"P","category":"c","price":1.00}]}
                    """.formatted(VALID_BUYER);

            mockMvc.perform(post("/api/payments/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("malformed JSON → 400")
        void malformed() throws Exception {
            mockMvc.perform(post("/api/payments/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("not-json"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("missing body → 400")
        void missingBody() throws Exception {
            mockMvc.perform(post("/api/payments/initiate")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("service IllegalStateException → 422")
        void illegalState() throws Exception {
            when(paymentService.initiate(any()))
                    .thenThrow(new IllegalStateException("provider down"));

            mockMvc.perform(post("/api/payments/initiate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_INITIATE_BODY))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("provider down"));
        }
    }

    // ─────────────────────────── POST /api/payments/callback/{method} ──

    @Nested
    @DisplayName("POST /api/payments/callback/{method}")
    class Callback {

        @Test
        @DisplayName("accepted result → 302 redirect with payment=success")
        void accepted() throws Exception {
            when(paymentService.handleCallback(eq("iyzico"), eq("tok"), eq(5L)))
                    .thenReturn(new VerifyPaymentResult(5L, true, null, "iyz-1"));

            mockMvc.perform(post("/api/payments/callback/iyzico")
                            .param("token", "tok")
                            .param("orderId", "5"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", containsString("payment=success")))
                    .andExpect(header().string("Location", containsString("orderId=5")));
        }

        @Test
        @DisplayName("rejected result → 302 redirect with payment=failed and reason")
        void rejected() throws Exception {
            when(paymentService.handleCallback(eq("iyzico"), eq("tok"), eq(5L)))
                    .thenReturn(new VerifyPaymentResult(5L, false, "declined", null));

            mockMvc.perform(post("/api/payments/callback/iyzico")
                            .param("token", "tok")
                            .param("orderId", "5"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", containsString("payment=failed")))
                    .andExpect(header().string("Location", containsString("reason=declined")));
        }

        @Test
        @DisplayName("service throws → 302 redirect with callback_error reason")
        void serviceError() throws Exception {
            when(paymentService.handleCallback(eq("iyzico"), eq("tok"), eq(5L)))
                    .thenThrow(new IllegalStateException("boom"));

            mockMvc.perform(post("/api/payments/callback/iyzico")
                            .param("token", "tok")
                            .param("orderId", "5"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", containsString("payment=failed")))
                    .andExpect(header().string("Location", containsString("reason=callback_error")));
        }

        @Test
        @DisplayName("orderId optional → still redirects, no orderId in URL")
        void noOrderIdParam() throws Exception {
            when(paymentService.handleCallback(eq("iyzico"), eq("tok"), eq(null)))
                    .thenReturn(new VerifyPaymentResult(7L, true, null, "iyz"));

            mockMvc.perform(post("/api/payments/callback/iyzico")
                            .param("token", "tok"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", containsString("orderId=7")));
        }

        @Test
        @DisplayName("missing token → 400")
        void missingToken() throws Exception {
            mockMvc.perform(post("/api/payments/callback/iyzico"))
                    .andExpect(status().isBadRequest());
            verify(paymentService, never()).handleCallback(any(), any(), any());
        }
    }
}
