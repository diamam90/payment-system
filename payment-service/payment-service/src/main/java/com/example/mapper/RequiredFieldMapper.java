package com.example.mapper;

import com.example.entity.PaymentMethodRequiredField;
import com.example.payment.dto.RequiredField;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RequiredFieldMapper {

    RequiredField toResponse(PaymentMethodRequiredField paymentMethodRequiredField);
}
