package com.example.service.impl;

import com.example.entity.Payment;
import com.example.entity.PaymentMethod;
import com.example.entity.Status;
import com.example.exception.ExternalServiceUnavailableException;
import com.example.fake.api.PayoutApi;
import com.example.fake.api.TransactionApi;
import com.example.fake.dto.PayoutRequest;
import com.example.fake.dto.PayoutResponse;
import com.example.fake.dto.TransactionRequest;
import com.example.fake.dto.TransactionResponse;
import com.example.mapper.FakePaymentProviderMapper;
import com.example.payment.dto.PaymentRequest;
import com.example.repository.PaymentRepository;
import com.example.service.MethodService;
import com.example.service.PaymentService;
import com.example.util.TransactionRunner;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final MethodService methodService;
    private final PaymentRepository paymentRepository;
    private final PayoutApi payoutApi;
    private final TransactionApi transactionApi;
    private final TransactionRunner transactionRunner;
    private final FakePaymentProviderMapper fakePaymentProviderMapper;
    private final MeterRegistry meterRegistry;

    @Override
    public Payment create(@NotNull PaymentRequest request) {
        PaymentMethod method = methodService.findById(request.getMethodId());
        Payment payment = draft(request, method);
        paymentRepository.saveAndFlush(payment);
        meterRegistry.counter("payments", "status", "PENDING").increment();
        log.info("Платеж с идентификатором  {} создан, статус {}", payment.getId(), payment.getStatus());

        try {
            switch (method.getType()) {
                case PAYOUT -> {
                    PayoutRequest req = fakePaymentProviderMapper.payout(payment);
                    PayoutResponse response = executeRequest(() -> payoutApi.createPayout(req));
                    transferToSuccess(payment.getId(), response.getId().toString());
                }
                case TRANSACTION -> {
                    TransactionRequest req = fakePaymentProviderMapper.transaction(payment);
                    TransactionResponse response = executeRequest(() -> transactionApi.createTransaction(req));
                    transferToSuccess(payment.getId(), response.getId().toString());
                }
            }
            meterRegistry.counter("payments", "status", "SUCCESS").increment();
        } catch (ExternalServiceUnavailableException ex) {
            transferToFailed(payment.getId());
            meterRegistry.counter("payments", "status", "FAILED").increment();
            throw ex;
        }
        return payment;
    }

    @Override
    public void compensate(Long paymentId) {
        transferToFailed(paymentId);
        meterRegistry.counter("compensations").increment();
    }

    private Payment draft(PaymentRequest request, PaymentMethod method) {
        Payment payment = new Payment();
        payment.setPaymentMethod(method);
        payment.setInternalTransactionId(request.getInternalTransactionUid());
        payment.setCurrency(request.getCurrency());
        payment.setAmount(request.getAmount());
        payment.setStatus(Status.PENDING);

        return payment;
    }

    private void transferToSuccess(Long paymentId, String providerId) {
        Status success = Status.SUCCESS;
        boolean isTransferred = transactionRunner.requiredWithResult(() -> {
            Optional<Payment> optional = paymentRepository.findByIdPessimisticWrite(paymentId);
            if (optional.isEmpty()) {
                log.info("Платеж с идентификатором {} не найден", paymentId);
                return false;
            }
            Payment payment = optional.get();
            if (Status.PENDING != payment.getStatus()) {
                log.error("Платеж с идентификатором {} в финальном статусе {}, перевод не возможен", paymentId, payment.getStatus());
                return false;
            }

            payment.setExternalTransactionId(providerId);
            payment.setStatus(success);
            return true;
        });

        if (isTransferred) log.info("Платеж с идентификатором {} переведен в статус {}", paymentId, success);
    }

    private void transferToFailed(Long paymentId) {
        Status failed = Status.FAILED;
        boolean isTransferred = transactionRunner.requiredWithResult(() -> {
            Optional<Payment> optional = paymentRepository.findByIdPessimisticWrite(paymentId);
            if (optional.isEmpty()) {
                log.info("Платеж с идентификатором {} не найден", paymentId);
                return false;
            }
            Payment payment = optional.get();
            if (Status.PENDING != payment.getStatus()) {
                log.error("Платеж с идентификатором {} в финальном статусе {}, перевод не возможен", paymentId, payment.getStatus());
                return false;
            }
            payment.setStatus(failed);
            return true;
        });
        if (isTransferred) log.info("Платеж с идентификатором {} переведен в статус {}", paymentId, failed);
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
