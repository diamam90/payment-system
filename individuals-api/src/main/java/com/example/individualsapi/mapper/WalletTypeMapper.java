package com.example.individualsapi.mapper;

import com.example.individuals.dto.WalletTypeResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletTypeMapper {

    WalletTypeResponse toIndividualResponse(com.example.transaction.dto.WalletTypeResponse type);
}
