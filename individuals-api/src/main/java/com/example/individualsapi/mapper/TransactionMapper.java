package com.example.individualsapi.mapper;

import com.example.individuals.dto.TransactionStatusResponse;
import com.example.transaction.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;

@Mapper(componentModel = "spring", subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public interface TransactionMapper {

    TransactionFilterRequest filterRequest(com.example.individuals.dto.TransactionFilterRequest request);

    @SubclassMapping(source = com.example.individuals.dto.DepositInitRequest.class, target = DepositInitRequest.class)
    @SubclassMapping(source = com.example.individuals.dto.TransferInitRequest.class, target = TransferInitRequest.class)
    @SubclassMapping(source = com.example.individuals.dto.WithdrawalInitRequest.class, target = WithdrawalInitRequest.class)
    TransactionInitRequest transactionInitRequest(com.example.individuals.dto.TransactionInitRequest request);

    @SubclassMapping(source = com.example.individuals.dto.DepositConfirmRequest.class, target = DepositConfirmRequest.class)
    @SubclassMapping(source = com.example.individuals.dto.TransferConfirmRequest.class, target = TransferConfirmRequest.class)
    @SubclassMapping(source = com.example.individuals.dto.WithdrawalConfirmRequest.class, target = WithdrawalConfirmRequest.class)
    TransactionConfirmRequest transactionConfirmRequest(com.example.individuals.dto.TransactionConfirmRequest request);

    com.example.individuals.dto.TransactionInitResponse transactionInitResponse(TransactionInitResponse response);

    com.example.individuals.dto.TransactionConfirmResponse transactionConfirmResponse(TransactionConfirmResponse response);

    TransactionFilterRequest transactionFilterRequest(com.example.individuals.dto.TransactionFilterRequest request);


    DepositInitRequest depositInit(com.example.individuals.dto.DepositInitRequest request);

    TransferInitRequest transferInit(com.example.individuals.dto.TransferInitRequest request);

    WithdrawalInitRequest withdrawalInit(com.example.individuals.dto.WithdrawalInitRequest request);

    DepositConfirmRequest depositConfirm(com.example.individuals.dto.DepositConfirmRequest request);

    TransferConfirmRequest transferInit(com.example.individuals.dto.TransferConfirmRequest request);

    WithdrawalConfirmRequest withdrawalInit(com.example.individuals.dto.WithdrawalConfirmRequest request);

    TransactionStatusResponse toIndividualsResponse(com.example.transaction.dto.TransactionStatusResponse response);
}
