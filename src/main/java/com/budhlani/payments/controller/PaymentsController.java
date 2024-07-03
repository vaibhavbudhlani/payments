package com.budhlani.payments.controller;

import com.budhlani.payments.models.PaymentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class PaymentsController {

    private WebClient webClient = WebClient.create();

    @Value("${ledger.service.url}")
    private String ledgerServiceUrl;
    @Value("${wallet.service.url}")
    private String walletServiceUrl;

    @PostMapping("/process")
    public Mono<String> processPayment(@RequestBody PaymentRequest paymentRequest){
        System.out.println(paymentRequest);
        paymentRequest.setUniqueId(UUID.randomUUID().toString());
        return  callPaymentGateway(paymentRequest).flatMap(x-> {
            System.out.println(x);
            return Mono.zip(storeEventInLedger(x),storeReceiptInWallet(x)).flatMap(x1-> {
                return Mono.just("Getting Response in ledger and waller "+x1.getT1()+" "+x1.getT2());
            });

        }).onErrorResume(throwable -> Mono.just("error in retrieving data "+throwable));


    }


    private Mono<PaymentResponse> callPaymentGateway(PaymentRequest paymentRequest){
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setReceipt(UUID.randomUUID().toString());
        paymentResponse.setPaymentRequest(paymentRequest);
        return Mono.just(paymentResponse).delayElement(Duration.ofSeconds(1));
    }



    private Mono<String> storeEventInLedger(PaymentResponse paymentResponse) {
        return webClient.post()
                .uri(ledgerServiceUrl + "/ledger/events")
                .bodyValue(paymentResponse)
                .retrieve()
                .bodyToMono(String.class)
                .elapsed()
                .map(x-> this.getDataFromLedger(x,paymentResponse))
                .doOnSuccess(response -> {
                    paymentResponse.setLedgerResponse(response);
                    System.out.println("Event stored in ledger");
                })
                .doOnError(error -> System.out.println("Failed to store event in ledger: " + error));


    }

    private Mono<String> storeReceiptInWallet(PaymentResponse paymentResponse) {
        return webClient.post()
                .uri(walletServiceUrl + "/wallet/receipt")
                .bodyValue(paymentResponse)
                .retrieve()

                .bodyToMono(String.class)
                .elapsed()

                .map(x-> this.GetDataFromWallet(x,paymentResponse))
                .doOnSuccess(response -> System.out.println("Receipt stored in wallet"))
                .doOnError(error -> System.out.println("Failed to store receipt in wallet: " + error));

    }

    public  String GetDataFromWallet(Tuple2<Long,String> tuple2,PaymentResponse paymentResponse){
        System.out.println(tuple2.getT2());
        paymentResponse.setWalletResponse(tuple2.getT2());
        return tuple2.getT2();
    }

    public String getDataFromLedger(Tuple2<Long,String> tuple2,PaymentResponse paymentResponse){
        System.out.println(tuple2.getT2());
        paymentResponse.setWalletResponse(tuple2.getT2());
        return tuple2.getT2();
    }
}
