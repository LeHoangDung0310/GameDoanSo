package com.gamedoanso.service;

import com.gamedoanso.config.VnPayConfig;
import com.gamedoanso.dto.PaymentRequest;
import com.gamedoanso.dto.PaymentResponse;
import com.gamedoanso.entity.PaymentTransaction;
import com.gamedoanso.entity.User;
import com.gamedoanso.repository.PaymentTransactionRepository;
import com.gamedoanso.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final VnPayConfig vnPayConfig;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public PaymentResponse createPaymentUrl(String username, PaymentRequest request, String ipAddress) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String transactionRef = generateTransactionRef();

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .user(user)
                    .amount(VnPayConfig.TURN_PACKAGE_PRICE)
                    .orderInfo(request.getOrderInfo())
                    .transactionRef(transactionRef)
                    .status("PENDING")
                    .build();
            paymentTransactionRepository.save(transaction);

            // Xây dưng các tham số gửi lên VNPAY
            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", VnPayConfig.VERSION);
            vnpParams.put("vnp_Command", VnPayConfig.COMMAND);
            vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
            vnpParams.put("vnp_Amount", String.valueOf(VnPayConfig.TURN_PACKAGE_PRICE * 100)); // VNPAY số tiên * 100

            vnpParams.put("vnp_CurrCode", VnPayConfig.CURRENCY_CODE);
            vnpParams.put("vnp_TxnRef", transactionRef);
            vnpParams.put("vnp_OrderInfo", request.getOrderInfo());
            vnpParams.put("vnp_OrderType", VnPayConfig.ORDER_TYPE);
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
            vnpParams.put("vnp_IpAddr", ipAddress);
            // lấy múi giờ
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            // thời gian gd
            String createDate = formatter.format(calendar.getTime());
            vnpParams.put("vnp_CreateDate", createDate);
            // Expire time 15 minutes
            calendar.add(Calendar.MINUTE, 15);
            String expireDate = formatter.format(calendar.getTime());
            vnpParams.put("vnp_ExpireDate", expireDate);
            // tạo chữ ký
            String queryUrl = buildQueryUrl(vnpParams);
            String hashData = buildHashData(vnpParams);
            String vnpSecureHash = vnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
            // tạo url thanh toán
            queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
            String paymentUrl = vnPayConfig.getVnpUrl() + "?" + queryUrl;

            return PaymentResponse.builder()
                    .paymentUrl(paymentUrl)
                    .transactionRef(transactionRef)
                    .message("Payment URL created successfully")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error creating payment URL: " + e.getMessage(), e);
        }
    }

    @Transactional
    public String processPaymentReturn(Map<String, String> vnpParams) {
        try {
            // xác thực chữ ký
            String vnpSecureHash = vnpParams.get("vnp_SecureHash");
            vnpParams.remove("vnp_SecureHash");
            vnpParams.remove("vnp_SecureHashType");

            String hashData = buildHashData(vnpParams);
            String calculatedHash = vnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData);

            if (!calculatedHash.equals(vnpSecureHash)) {
                return "INVALID_SIGNATURE";
            }

            String transactionRef = vnpParams.get("vnp_TxnRef");
            String responseCode = vnpParams.get("vnp_ResponseCode");

            // transactionRef dùng để liên kết DB và VNPay gọi để xác thực biết gd thuộc
            // a,tránh trùng, cập nhật
            // mã giao dịch
            PaymentTransaction transaction = paymentTransactionRepository.findByTransactionRef(transactionRef)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            if (!"PENDING".equals(transaction.getStatus())) {
                return "ALREADY_PROCESSED";
            }

            // locgic xử lý + số lượng lượt chơi hiện tại của user
            if ("00".equals(responseCode)) {
                transaction.setStatus("SUCCESS");

                User user = transaction.getUser();
                user.setTurns(user.getTurns() + VnPayConfig.TURNS_PER_PACKAGE);
                userRepository.save(user);

                paymentTransactionRepository.save(transaction);
                return "SUCCESS";
            } else {

                transaction.setStatus("FAILED");
                paymentTransactionRepository.save(transaction);
                return "FAILED";
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing payment return: " + e.getMessage(), e);
        }
    }

    // Tạo mã giao dịch
    private String generateTransactionRef() {
        return "TXN" + System.currentTimeMillis();
    }

    // tạo URL cho trình duyệt
    private String buildQueryUrl(Map<String, String> params) throws UnsupportedEncodingException {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        // Sort theo alphabet
        Collections.sort(fieldNames);
        // dùng stringbuilder để ghép chuỗi
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                }
            }
        }
        return query.toString();
    }

    // tạo chuỗi để ký bảo mật
    private String buildHashData(Map<String, String> params) throws UnsupportedEncodingException {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        return hashData.toString();
    }
}
