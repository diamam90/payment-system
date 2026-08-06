package com.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(schema = "payment", name = "payouts")
public class Payout {

    @Id
    @Column(name = "id")
    @SequenceGenerator(schema = "payment", name = "payoutIdGenerator", sequenceName = "payouts_id_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payoutIdGenerator")
    private Long id;

    @Column(name = "merchant_id", insertable = false, updatable = false)
    private Integer merchantId;

    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "notification_url")
    private String notificationUrl;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void setMerchant(Merchant merchant) {
        if (merchant != null) {
            this.merchantId = merchant.getId();
        }
        this.merchant = merchant;
    }

}
