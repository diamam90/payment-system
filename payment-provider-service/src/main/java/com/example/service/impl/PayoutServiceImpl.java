package com.example.service.impl;

import com.example.entity.Merchant;
import com.example.entity.Payout;
import com.example.exception.ObjectNotFoundException;
import com.example.fake.dto.PayoutRequest;
import com.example.mapper.PayoutMapper;
import com.example.repository.PayoutRepository;
import com.example.service.MerchantService;
import com.example.service.PayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private final MerchantService merchantService;
    private final PayoutRepository payoutRepository;
    private final PayoutMapper payoutMapper;

    @Override
    public Payout create(PayoutRequest request, Authentication auth) {
        Merchant merchant = (Merchant) auth.getPrincipal();
        Payout payout = payoutMapper.create(request);
        payout.setMerchant(merchant);
        log.debug("Транзакция на выплату успешно создана, id: {}", payout.getId());
        return payoutRepository.save(payout);
    }

    @Override
    @Transactional(readOnly = true)
    public Payout getById(Long id, Authentication auth) {
        Merchant merchant = (Merchant) auth.getPrincipal();
        return payoutRepository.findByIdAndMerchantId(id, merchant.getId())
                .orElseThrow(() -> new ObjectNotFoundException("Payout", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payout> findBy(ZonedDateTime start, ZonedDateTime end, Authentication auth) {
        Merchant merchant = (Merchant) auth.getPrincipal();
        return payoutRepository.findByMerchantIdAndCreatedAtBetween(merchant.getId(), start.toLocalDateTime(), end.toLocalDateTime());
    }
}
