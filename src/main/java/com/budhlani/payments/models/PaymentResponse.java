package com.budhlani.payments.models;

import com.budhlani.payments.controller.PaymentRequest;
import lombok.Data;

@Data
public class PaymentResponse {

    private String receipt;
    private String ledgerResponse;
    private String walletResponse;
    private PaymentRequest paymentRequest;
}
