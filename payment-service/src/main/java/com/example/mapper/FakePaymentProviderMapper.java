package com.example.mapper;

import com.example.entity.Payment;
import com.example.fake.dto.PayoutRequest;
import com.example.fake.dto.TransactionRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FakePaymentProviderMapper {

    @Mapping(source = "internalTransactionId", target = "externalId")
    PayoutRequest payout(Payment payment);

    @Mapping(source = "internalTransactionId", target = "externalId")
    @Mapping(source = "paymentMethod.providerMethodType", target = "method")
    TransactionRequest transaction(Payment payment);
}
