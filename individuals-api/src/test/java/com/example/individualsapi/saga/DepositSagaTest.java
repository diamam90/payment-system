package com.example.individualsapi.saga;

import com.example.individuals.dto.DepositConfirmRequest;
import com.example.individuals.dto.TransactionConfirmResponse;
import com.example.individuals.dto.WalletResponse;
import com.example.individuals.dto.WalletTypeResponse;
import com.example.individualsapi.service.PaymentService;
import com.example.individualsapi.service.TransactionService;
import com.example.individualsapi.service.WalletService;
import com.example.payment.dto.PaymentMethodFilter;
import com.example.payment.dto.PaymentMethodResponse;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositSagaTest {

    @Mock
    private TransactionService transactionService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private WalletService walletService;
    @InjectMocks
    private DepositSaga saga;

    @Test
    void process() {
        // given
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID walletId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID transactionId = UUID.fromString("00000000-0000-0000-0000-000000000666");

        String providerTransactionId = "DEPOSIT-228";
        String countryCode = "RUS";
        String currencyCode = "EUR";

        Integer methodId = 666;

        BigDecimal amount = BigDecimal.valueOf(295.99).setScale(2, RoundingMode.HALF_EVEN);

        DepositConfirmRequest request = new DepositConfirmRequest()
                .amount(BigDecimal.valueOf(500.00).setScale(2, RoundingMode.HALF_EVEN))
                .comment("DEPOSIT for all")
                .userUid(userId)
                .walletUid(walletId)
                .type("DEPOSIT");

        WalletResponse walletResponse = new WalletResponse()
                .name("test wallet")
                .userUid(userId)
                .balance(BigDecimal.valueOf(300.00).setScale(2, RoundingMode.HALF_EVEN))
                .status("active")
                .type(new WalletTypeResponse()
                        .currencyCode(currencyCode));

        PaymentMethodFilter filter = new PaymentMethodFilter()
                .countryCode(countryCode)
                .currencyCode(currencyCode)
                .type(PaymentMethodFilter.TypeEnum.TRANSACTION);

        PaymentMethodResponse method = new PaymentMethodResponse()
                .providerMethodType("fake provider type 1")
                .id(methodId);

        PaymentRequest paymentRequest = new PaymentRequest()
                .amount(amount)
                .internalTransactionUid(transactionId)
                .methodId(methodId);

        PaymentResponse paymentResponse = new PaymentResponse()
                .amount(amount)
                .internalTransactionUid(transactionId)
                .providerTransactionId(providerTransactionId)
                .status("SUCCESS");

        TransactionConfirmResponse transactionResponse = new TransactionConfirmResponse()
                .amount(amount)
                .status("PENDING")
                .uid(transactionId);

        when(walletService.getById(walletId)).thenReturn(Mono.just(walletResponse));
        when(transactionService.transactionConfirm(request)).thenReturn(Mono.just(transactionResponse));
        when(paymentService.getMethod(eq(filter))).thenReturn(Mono.just(method));
        when(paymentService.createPayment(paymentRequest)).thenReturn(Mono.just(paymentResponse));
        when(transactionService.success(transactionId, providerTransactionId, userId)).thenReturn(Mono.empty());

        TransactionConfirmResponse response = saga.process(request).block();

        assertEquals(transactionResponse, response);
    }
}