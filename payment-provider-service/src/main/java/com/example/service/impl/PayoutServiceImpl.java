package com.example.service.impl;

import com.example.entity.Merchant;
import com.example.entity.Payout;
import com.example.exception.BadRequestException;
import com.example.exception.ObjectNotFoundException;
import com.example.fake.dto.PayoutRequest;
import com.example.mapper.PayoutMapper;
import com.example.repository.PayoutRepository;
import com.example.service.PayoutService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private final PayoutRepository payoutRepository;
    private final PayoutMapper payoutMapper;
    private final Clock clock;

    @Override
    public Payout create(PayoutRequest request, Merchant merchant) {
        Payout payout = payoutMapper.create(request);
        payout.setMerchant(merchant);
        payoutRepository.save(payout);
        log.info("Транзакция на выплату успешно создана, id: {}", payout.getId());
        return payout;
    }

    @Override
    @Transactional(readOnly = true)
    public Payout getById(Long id, Integer merchantId) {
        return payoutRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new ObjectNotFoundException("Payout", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payout> findBy(@Nullable ZonedDateTime start, @Nullable ZonedDateTime end, Integer merchantId) {
        validateDate(start, end);

        if (start == null && end == null) {
            int currentYear = ZonedDateTime.now(clock).getYear();
            start = ZonedDateTime.of(currentYear, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            end = ZonedDateTime.of(currentYear, 12, 31, 23, 59, 59, 999, ZoneOffset.UTC);
        } else if (start == null) {
            int currentYear = end.getYear();
            start = ZonedDateTime.of(currentYear, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        } else if (end == null) {
            int currentYear = start.getYear();
            end = ZonedDateTime.of(currentYear, 12, 31, 23, 59, 59, 999, ZoneOffset.UTC);
        }

        LocalDateTime startDateTime = start.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime endDateTime = end.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        return payoutRepository.findByMerchantIdAndCreatedAtBetween(merchantId, startDateTime, endDateTime);
    }

    @Transactional(readOnly = true)
    public Optional<Payout> findByIdPessimisticWrite(Long id) {
        return payoutRepository.findByIdPessimisticWrite(id);
    }

    private void validateDate(ZonedDateTime start, ZonedDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("end_date must be after start_date");
        }
    }
}
