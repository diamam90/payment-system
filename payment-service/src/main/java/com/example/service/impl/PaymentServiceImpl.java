package com.example.service.impl;

import com.example.client.FakeProviderClient;
import com.example.entity.Payment;
import com.example.entity.PaymentMethod;
import com.example.entity.PaymentMethodDefinition;
import com.example.entity.Status;
import com.example.exception.ExternalServiceUnavailableException;
import com.example.fake.dto.PayoutRequest;
import com.example.fake.dto.PayoutResponse;
import com.example.fake.dto.TransactionRequest;
import com.example.fake.dto.TransactionResponse;
import com.example.mapper.FakePaymentProviderMapper;
import com.example.model.Merchant;
import com.example.payment.dto.PaymentRequest;
import com.example.repository.PaymentRepository;
import com.example.service.MethodService;
import com.example.service.PaymentService;
import com.example.util.TransactionRunner;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final MethodService methodService;
    private final PaymentRepository paymentRepository;
    private final FakeProviderClient fakeProviderClient;
    private final TransactionRunner transactionRunner;
    private final FakePaymentProviderMapper fakePaymentProviderMapper;
    private final MeterRegistry meterRegistry;

    private static final String SUCCESS_STATUS = "SUCCESS";

    @Override
    public Payment create(@NotNull PaymentRequest request, Merchant merchant) {
        Optional<Payment> optionalPayment = paymentRepository.findByInternalTransactionId(request.getInternalTransactionUid());
        if (optionalPayment.isPresent()) {
            log.info("Платеж с внутренним идентификатором {} уже существует", request.getInternalTransactionUid());
            return optionalPayment.get();
        }

        PaymentMethod method = methodService.findActiveById(request.getMethodId());
        Payment payment = draft(request, method, merchant.getUsername());
        paymentRepository.saveAndFlush(payment);
        meterRegistry.counter("payments", "status", "PENDING").increment();
        log.info("Платеж с идентификатором  {} создан, статус {}", payment.getId(), payment.getStatus());

        try {
            switch (method.getType()) {
                case PAYOUT -> processPayout(payment);
                case TRANSACTION -> processTransaction(payment);
            }
        } catch (ExternalServiceUnavailableException ex) {
            transferToFailed(payment.getId());
            throw ex;
        }
        return payment;
    }

    @Override
    public void compensate(Long paymentId) {
        transferToFailed(paymentId);
        meterRegistry.counter("compensations").increment();
    }

    private Payment draft(PaymentRequest request, PaymentMethod method, String merchantId) {
        Payment payment = new Payment();
        payment.setPaymentMethod(method);
        payment.setInternalTransactionId(request.getInternalTransactionUid());
        payment.setAmount(request.getAmount());
        payment.setStatus(Status.PENDING);
        payment.setMerchantId(merchantId);

        Optional.ofNullable(method)
                .map(PaymentMethod::getPaymentMethodDefinition)
                .map(PaymentMethodDefinition::getCurrencyCode)
                .ifPresent(payment::setCurrency);

        return payment;
    }

    private void processPayout(Payment payment) {
        PayoutRequest req = fakePaymentProviderMapper.payout(payment);
        PayoutResponse response = fakeProviderClient.createPayout(req);
        if (SUCCESS_STATUS.equals(response.getStatus())) {
            transferToSuccess(payment.getId(), response.getId().toString());
        } else {
            log.error("Ответ от провайдер сервиса не подтвердил выплату, статус: {}", response.getStatus());
            transferToFailed(payment.getId(), String.valueOf(response.getId()));
        }
    }

    private void processTransaction(Payment payment) {
        TransactionRequest req = fakePaymentProviderMapper.transaction(payment);
        TransactionResponse response = fakeProviderClient.createTransaction(req);
        if (SUCCESS_STATUS.equals(response.getStatus())) {
            transferToSuccess(payment.getId(), response.getId().toString());
        } else {
            log.error("Ответ от провайдер сервиса не подтвердил пополнение, статус: {}", response.getStatus());
            transferToFailed(payment.getId(), String.valueOf(response.getId()));
        }
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

        if (isTransferred) {
            log.info("Платеж с идентификатором {} переведен в статус {}", paymentId, success);
            meterRegistry.counter("payments", "status", "SUCCESS").increment();
        }
    }

    private void transferToFailed(Long paymentId) {
        transferToFailed(paymentId, null);
    }

    private void transferToFailed(Long paymentId, @Nullable String externalTransactionId) {
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
            if (externalTransactionId != null) payment.setExternalTransactionId(externalTransactionId);
            payment.setStatus(failed);
            return true;
        });
        if (isTransferred) {
            log.info("Платеж с идентификатором {} переведен в статус {}", paymentId, failed);
            meterRegistry.counter("payments", "status", "FAILED").increment();
        }
    }
}
