package com.example.individualsapi.mapper;

import com.example.individuals.dto.TransactionStatusResponse;
import com.example.transaction.dto.*;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring", subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public interface TransactionMapper {

    @Named("scale")
    default BigDecimal scale(BigDecimal value){
        if (value== null) return null;
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    TransactionFilterRequest filterRequest(com.example.individuals.dto.TransactionFilterRequest request);

    @Mapping(source = "amount", target = "amount", qualifiedByName = "scale")
    @Mapping(source = "fee", target = "fee", qualifiedByName = "scale")
    com.example.individuals.dto.TransactionInitResponse transactionInitResponse(TransactionInitResponse response);

    @Mapping(source = "amount", target = "amount", qualifiedByName = "scale")
    @Mapping(source = "fee", target = "fee", qualifiedByName = "scale")
    com.example.individuals.dto.TransactionConfirmResponse transactionConfirmResponse(TransactionConfirmResponse response);

    TransactionFilterRequest transactionFilterRequest(com.example.individuals.dto.TransactionFilterRequest request);


    DepositInitRequest depositInit(com.example.individuals.dto.DepositInitRequest request);

    @Mapping(target = "rate", source = "rate")
    TransferInitRequest transferInit(com.example.individuals.dto.TransferInitRequest request, BigDecimal rate);

    WithdrawalInitRequest withdrawalInit(com.example.individuals.dto.WithdrawalInitRequest request);

    DepositConfirmRequest depositConfirm(com.example.individuals.dto.DepositConfirmRequest request);

    @Mapping(target = "rate", source = "rate")
    TransferConfirmRequest transferConfirm(com.example.individuals.dto.TransferConfirmRequest request, BigDecimal rate);

    WithdrawalConfirmRequest withdrawalConfirm(com.example.individuals.dto.WithdrawalConfirmRequest request);

    TransactionStatusResponse toIndividualsResponse(com.example.transaction.dto.TransactionStatusResponse response);
}
