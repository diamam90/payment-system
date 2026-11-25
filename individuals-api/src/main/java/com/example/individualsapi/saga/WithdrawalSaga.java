package com.example.individualsapi.saga;

import com.example.individuals.dto.TransactionConfirmResponse;
import com.example.individuals.dto.WalletResponse;
import com.example.individuals.dto.WalletTypeResponse;
import com.example.individuals.dto.WithdrawalConfirmRequest;
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
public class WithdrawalSaga {

    private final WalletService walletService;
    private final TransactionService transactionService;
    private final PaymentService paymentService;

    private static final String DEFAULT_COUNTRY_CODE = "RUS";

    public Mono<TransactionConfirmResponse> process(WithdrawalConfirmRequest request) {
        return walletService.getById(request.getWalletUid())
                .map(WalletResponse::getType)
                .map(WalletTypeResponse::getCurrencyCode)
                .flatMap(currencyCode -> {
                    PaymentMethodFilter filter = withdrawal(currencyCode);

                    return Mono.zip(
                            paymentService.getMethod(filter),
                            transactionService.transactionConfirm(request)
                    );
                })
                .flatMap(tuple -> {
                    PaymentRequest paymentRequest = paymentRequest(tuple);

                    return Mono.zip(
                            paymentService.createPayment(paymentRequest)
                                    .doOnSuccess(payment -> transactionService.success(tuple.getT2().getUid(), payment.getProviderTransactionId(), request.getUserUid()))
                                    .doOnError(err -> transactionService.failed(tuple.getT2().getUid(), tuple.getT2().getUserUid())),
                            Mono.just(tuple.getT2()));
                })
                .map(Tuple2::getT2)
                .doOnSuccess(transaction -> transaction.setStatus("SUCCESS"));
    }

    private PaymentMethodFilter withdrawal(String currencyCode) {
        return new PaymentMethodFilter()
                .countryCode(DEFAULT_COUNTRY_CODE)
                .currencyCode(currencyCode)
                .type(PaymentMethodFilter.TypeEnum.PAYOUT);
    }

    private PaymentRequest paymentRequest(Tuple2<PaymentMethodResponse, TransactionConfirmResponse> tuple) {
        return new PaymentRequest()
                .internalTransactionUid(tuple.getT2().getUid())
                .methodId(tuple.getT1().getId())
                .amount(tuple.getT2().getAmount());
    }
}
