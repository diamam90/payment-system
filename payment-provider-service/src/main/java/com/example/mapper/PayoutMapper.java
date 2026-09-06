package com.example.mapper;

import com.example.entity.Payout;
import com.example.fake.dto.PayoutRequest;
import com.example.fake.dto.PayoutResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ValueMapping;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Mapper(componentModel = "spring", uses = DateTimeMapper.class)
public interface PayoutMapper {

    @Mapping(target = "status", constant = "PENDING")
    Payout create(PayoutRequest request);

    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = {"DateTimeMapper", "toZDT"})
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = {"DateTimeMapper", "toZDT"})
    @Mapping(target = "merchantId", source = "payout.merchant.merchantId")
    PayoutResponse toResponse(Payout payout);
}
