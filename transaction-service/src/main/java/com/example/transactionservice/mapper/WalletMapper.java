package com.example.transactionservice.mapper;

import com.example.transaction.dto.WalletResponse;
import com.example.transactionservice.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Mapper(componentModel = "spring", uses = WalletTypeMapper.class)
public interface WalletMapper {

    ZoneOffset defaultZone = ZoneOffset.UTC;

    @Named("instantToZonedDateTime")
    default ZonedDateTime instantToZonedDateTime(Instant instant) {
        if (instant == null) return null;
        return instant.atZone(defaultZone);
    }

    @Named("bigDecimalRound")
    default BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    @Mappings({
            @Mapping(source = "wallet.type", target = "type"),
            @Mapping(source = "archivedAt", target = "archivedAt", qualifiedByName = "instantToZonedDateTime"),
            @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "instantToZonedDateTime"),
            @Mapping(source = "id", target = "uid"),
            @Mapping(source = "userId", target = "userUid"),
            @Mapping(source = "balance", target = "balance", qualifiedByName = "bigDecimalRound")
    })
    WalletResponse toResponse(Wallet wallet);
}
