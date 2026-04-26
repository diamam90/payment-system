package com.example.transactionservice.mapper;

import com.example.transaction.dto.WalletTypeResponse;
import com.example.transactionservice.entity.WalletType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletTypeMapper {

    @Mapping(target = "uid", source = "id")
    WalletTypeResponse toResponse(WalletType type);
}
