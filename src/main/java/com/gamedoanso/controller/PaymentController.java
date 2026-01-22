package com.gamedoanso.controller;

import com.gamedoanso.dto.PaymentRequest;
import com.gamedoanso.dto.PaymentResponse;
import com.gamedoanso.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String username = authentication.getName();
        String ipAddress = getClientIp(httpRequest);

        PaymentResponse response = paymentService.createPaymentUrl(username, request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<Map<String, String>> vnpayReturn(@RequestParam Map<String, String> params) {
        String result = paymentService.processPaymentReturn(params);

        Map<String, String> response = new HashMap<>();
        response.put("status", result);

        if ("SUCCESS".equals(result)) {
            response.put("message", "Payment successful! 5 turns have been added to your account.");
        } else if ("FAILED".equals(result)) {
            response.put("message", "Payment failed. Please try again.");
        } else if ("INVALID_SIGNATURE".equals(result)) {
            response.put("message", "Invalid payment signature.");
        } else if ("ALREADY_PROCESSED".equals(result)) {
            response.put("message", "This transaction has already been processed.");
        }

        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}
