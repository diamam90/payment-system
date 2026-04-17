package com.example.individualsapi.mapper;

import com.example.individuals.dto.WalletResponse;
import com.example.transaction.dto.CreateWalletRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = WalletTypeMapper.class)
public interface WalletMapper {

    CreateWalletRequest toWalletServiceRequest(com.example.individuals.dto.CreateWalletRequest request);

    WalletResponse toIndividualApiResponse(com.example.transaction.dto.WalletResponse response);
}
