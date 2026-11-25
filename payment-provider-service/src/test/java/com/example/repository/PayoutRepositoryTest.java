package com.example.repository;

import com.example.entity.Merchant;
import com.example.entity.Status;
import com.example.entity.Payout;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public class PayoutRepositoryTest {
    
    @Autowired  
    private MerchantRepository merchantRepository;
    @Autowired
    private PayoutRepository payoutRepository;

    @Sql("/sql/payout-with-external-id.sql")
    @Test
    void savePayout_whenExternalIdAlreadyExists_shouldReturnException() {
        Merchant merchant = merchantRepository.findByMerchantId("merchant1").get();
        Payout payout = new Payout();
        payout.setExternalId("external id 228");
        payout.setAmount(BigDecimal.TEN);
        payout.setStatus(Status.PENDING);
        payout.setMerchant(merchant);
        assertThatThrownBy(() -> payoutRepository.save(payout))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
    
}
