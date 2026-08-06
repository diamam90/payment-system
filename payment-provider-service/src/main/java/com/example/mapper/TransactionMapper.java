package com.example.mapper;

import com.example.entity.Transaction;
import com.example.fake.dto.TransactionRequest;
import com.example.fake.dto.TransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Mapper(componentModel = "spring", uses = DateTimeMapper.class)
public interface TransactionMapper {

    @Mapping(target = "status", constant = "PENDING")
    Transaction create(TransactionRequest request);

    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = {"DateTimeMapper", "toZDT"})
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = {"DateTimeMapper", "toZDT"})
    @Mapping(target = "merchantId", source = "transaction.merchant.merchantId")
    TransactionResponse toResponse(Transaction transaction);

}
