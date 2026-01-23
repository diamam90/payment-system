package com.example.transactionservice.entity;

import com.example.transactionservice.model.WalletStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@Table(schema = "transaction", name = "wallets")
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uid")
    private UUID id;

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "wallet_type_uid", insertable = false, updatable = false)
    private WalletType type;

    @Column(name = "wallet_type_uid")
    private UUID walletTypeId;

    @Column(name = "user_uid", updatable = false)
    private UUID userId;

    @Column(name = "status")
    private String status;

    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @OneToMany(mappedBy = "wallet")
    private Set<Transaction> transactions;

    @OneToMany(mappedBy = "targetWallet")
    private Set<Transaction> targetTransactions;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "modified_at")
    @UpdateTimestamp
    private Instant modifiedAt;

    public void setType(WalletType walletType) {
        if (walletType != null && walletType.getId() != null) {
            this.walletTypeId = walletType.getId();
            this.type = walletType;
        }
    }

    public static class WalletBuilder {
        private WalletType type;
        private UUID walletTypeId;

        public WalletBuilder type(WalletType type) {
            this.type = type;
            this.walletTypeId = type.getId();
            return this;
        }

        public WalletBuilder balance(BigDecimal balance) {
            this.balance = balance.setScale(2, RoundingMode.HALF_UP);
            return this;
        }
    }

    public boolean isInactive() {
        return WalletStatus.INACTIVE.getCode().equals(this.getStatus());
    }

    public void increaseBalance(BigDecimal value) {
        this.balance = this.balance.add(value).setScale(2, RoundingMode.HALF_UP);
    }

    public void decreaseBalance(BigDecimal value) {
        this.balance = this.balance.subtract(value).setScale(2, RoundingMode.HALF_UP);
    }

    public void setBalance(BigDecimal value) {
        this.balance = value.setScale(2, RoundingMode.HALF_UP);
    }

}
