package com.example.transactionservice.mapper;

import com.example.transaction.dto.TransactionConfirmResponse;
import com.example.transaction.dto.TransactionStatusResponse;
import com.example.transactionservice.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    ZoneOffset defaultZone = ZoneOffset.UTC;

    @Named("instantToZonedDateTime")
    default ZonedDateTime instantToZonedDateTime(Instant value) {
        if (value == null) return null;
        return value.atZone(defaultZone);
    }

    @Mapping(source = "id", target = "uid")
    @Mapping(source = "userId", target = "userUid")
    @Mapping(source = "walletId", target = "walletUid")
    @Mapping(source = "targetWalletId", target = "targetWalletUid")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "instantToZonedDateTime")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "instantToZonedDateTime")
    TransactionConfirmResponse toResponse(Transaction transaction);

    @Mapping(source = "id", target = "uid")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "instantToZonedDateTime")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "instantToZonedDateTime")
    TransactionStatusResponse toTransactionStatusResponse(Transaction transaction);
}
