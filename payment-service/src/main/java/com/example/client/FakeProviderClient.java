package com.example.client;

import com.example.exception.ExternalServiceUnavailableException;
import com.example.fake.api.PayoutApi;
import com.example.fake.api.TransactionApi;
import com.example.fake.dto.PayoutRequest;
import com.example.fake.dto.PayoutResponse;
import com.example.fake.dto.TransactionRequest;
import com.example.fake.dto.TransactionResponse;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class FakeProviderClient {

    private final PayoutApi payoutApi;
    private final TransactionApi transactionApi;

    @NewSpan("fake_payment_provider.create_payout")
    public PayoutResponse createPayout(PayoutRequest request) {
        return executeRequest(() -> payoutApi.createPayout(request));
    }

    @NewSpan("fake_payment_provider.create_transaction")
    public TransactionResponse createTransaction(TransactionRequest request) {
        return executeRequest(() -> transactionApi.createTransaction(request));
    }

    private <R> R executeRequest(Supplier<ResponseEntity<R>> executable) {
        try {
            return executable.get().getBody();
        } catch (RestClientException ex) {
            log.error("FakeProviderProvider вернул ошибку: {}", ex.getMessage(), ex);
            throw new ExternalServiceUnavailableException("FakePaymentProvider");
        }
    }
}
