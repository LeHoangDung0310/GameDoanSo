package com.gamedoanso.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private String paymentUrl;
    private String transactionRef;
    private String message;
}
