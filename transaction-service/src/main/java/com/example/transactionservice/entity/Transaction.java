package com.example.transactionservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(schema = "transaction", name = "transactions")
public class Transaction {

    @Id
    @Column(name = "uid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_uid", updatable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "wallet_uid")
    private Wallet wallet;

    @Column(name = "wallet_uid", insertable = false, updatable = false)
    private UUID walletId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status;

    @Column(name = "comment")
    private String comment;

    @Column(name = "fee")
    private BigDecimal fee;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "target_wallet_uid")
    private Wallet targetWallet;

    @Column(name = "target_wallet_uid", insertable = false, updatable = false)
    private UUID targetWalletId;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public void setWallet(Wallet wallet) {
        if (wallet == null) {
            this.walletId = null;
            this.wallet = null;
        } else {
            this.walletId = wallet.getId();
            this.wallet = wallet;
        }
    }

    public void setTargetWallet(Wallet wallet) {
        if (wallet == null) {
            this.targetWalletId = null;
            this.targetWallet = null;
        } else {
            this.targetWalletId = wallet.getId();
            this.targetWallet = wallet;
        }
    }

    public boolean isFinished() {
        return this.status.equals(TransactionStatus.COMPLETED) || this.status.equals(TransactionStatus.FAILED);
    }
}
