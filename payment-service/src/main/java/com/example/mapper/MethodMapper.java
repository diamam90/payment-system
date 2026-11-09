package com.example.mapper;

import com.example.entity.PaymentMethod;
import com.example.payment.dto.PaymentMethodResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MethodMapper {

    PaymentMethodResponse toResponse(PaymentMethod method);
}
