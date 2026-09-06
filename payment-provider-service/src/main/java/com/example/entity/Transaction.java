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
@Table(schema = "payment", name = "transactions")
public class Transaction {

    @Id
    @Column(name = "id")
    @SequenceGenerator(schema = "payment", name = "transactionIdGenerator", sequenceName = "transactions_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transactionIdGenerator")
    private Long id;

    @Column(name = "merchant_id", updatable = false, insertable = false)
    private Integer merchantId;

    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "description")
    private String description;

    @Column(name = "method")
    private String method;

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

    public boolean isFinalStatus() {
        return Status.FAILED.equals(status) || Status.SUCCESS.equals(status);
    }
}
