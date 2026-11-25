package com.example.mapper;

import com.example.entity.PaymentMethod;
import com.example.payment.dto.PaymentMethodResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MethodMapper {

    @Mapping(target = "imageUrl", source = "logo")
    @Mapping(target = "requiredFields", source = "method.paymentMethodRequiredFields")
    PaymentMethodResponse toResponse(PaymentMethod method);
}
