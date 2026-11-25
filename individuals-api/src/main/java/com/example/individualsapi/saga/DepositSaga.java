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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Component
@RequiredArgsConstructor
public class DepositSaga {

    private final TransactionService transactionService;
    private final PaymentService paymentService;
    private final WalletService walletService;

    private static final String DEFAULT_COUNTRY_CODE = "RUS";

    public Mono<TransactionConfirmResponse> process(DepositConfirmRequest request) {
        return walletService.getById(request.getWalletUid())
                .map(WalletResponse::getType)
                .map(WalletTypeResponse::getCurrencyCode)
                .flatMap(currencyCode -> {
                    PaymentMethodFilter filter = deposit(currencyCode);

                    return Mono.zip(
                            transactionService.transactionConfirm(request),
                            paymentService.getMethod(filter)
                    );
                })
                .flatMap(tuple -> {
                    PaymentRequest paymentRequest = paymentRequest(tuple);
                    return Mono.zip(
                            paymentService.createPayment(paymentRequest)
                                    .doOnSuccess(payment -> transactionService.success(tuple.getT1().getUid(), payment.getProviderTransactionId(), request.getUserUid()))
                                    .doOnError(err -> transactionService.failed(tuple.getT1().getUid(), tuple.getT1().getUserUid())),
                            Mono.just(tuple.getT1()));
                })
                .map(Tuple2::getT2)
                .doOnSuccess(transaction -> transaction.setStatus("SUCCESS"));
    }

    private PaymentMethodFilter deposit(String currencyCode) {
        return new PaymentMethodFilter()
                .countryCode(DEFAULT_COUNTRY_CODE)
                .currencyCode(currencyCode)
                .type(PaymentMethodFilter.TypeEnum.TRANSACTION);
    }

    private PaymentRequest paymentRequest(Tuple2<TransactionConfirmResponse, PaymentMethodResponse> tuple) {
        return new PaymentRequest()
                .internalTransactionUid(tuple.getT1().getUid())
                .methodId(tuple.getT2().getId())
                .amount(tuple.getT1().getAmount());
    }
}
