package com.example.service;

import com.example.entity.Merchant;
import com.example.entity.Payout;
import com.example.fake.dto.PayoutRequest;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PayoutService {

    Payout create(PayoutRequest request, Merchant merchant);

    Payout getById(Long id, Integer merchantId);

    /**
     * Поиск выплат между указанными датами для конкретного клиента,
     * если даты не указаны, то поиск будет по текущему году,
     * если указана дата начала интервала, то поиск будет до конца указанного года
     * если указана дата окончания интервала, то поиск будет с начала указанного года
     *
     * @param start      начало интервала
     * @param end        конец интервала
     * @param merchantId идентификатор клиента
     * @return
     */
    List<Payout> findBy(@Nullable ZonedDateTime start, @Nullable ZonedDateTime end, Integer merchantId);

    Optional<Payout> findByIdPessimisticWrite(Long id);
}
