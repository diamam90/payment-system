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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private final PayoutRepository payoutRepository;
    private final PayoutMapper payoutMapper;

    @Override
    public Payout create(PayoutRequest request, Merchant merchant) {
        Optional<Payout> optional = payoutRepository.findByExternalId(request.getExternalId());
        if (optional.isPresent()){
            Payout payout = optional.get();
            if (!merchant.getId().equals(payout.getMerchantId())){
                throw new BadRequestException("Payout with external id [%s] already exists".formatted(request.getExternalId()));
            }
            return optional.get();
        }

        Payout payout = payoutMapper.create(request);
        payout.setMerchant(merchant);
        payoutRepository.save(payout);
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
            return payoutRepository.findByMerchantId(merchantId);
        } else if (start == null) {
            LocalDateTime endDateTime = end.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            return payoutRepository.findByMerchantIdAndCreatedAtBefore(merchantId, endDateTime);
        } else if (end == null) {
            LocalDateTime startDateTime = start.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            return payoutRepository.findByMerchantIdAndCreatedAtAfter(merchantId, startDateTime);
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
