package com.example.mapper;

import com.example.entity.Payment;
import com.example.payment.dto.PaymentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = RequiredFieldMapper.class)
public interface PaymentMapper {

    @Mapping(source = "externalTransactionId", target = "providerTransactionId")
    @Mapping(source = "internalTransactionId", target = "internalTransactionUid")
    PaymentResponse toResponse(Payment payment);
}
