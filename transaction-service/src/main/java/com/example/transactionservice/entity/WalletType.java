package com.example.transactionservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(schema = "transaction", name = "wallet_types")
public class WalletType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uid")
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "status")
    private String status;

    @Column(name = "user_type")
    private String userType;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "creator")
    private String creator;

    @Column(name = "modifier")
    private String modifier;

    @CreatedDate
    @Column(name = "created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "type")
    private Set<Wallet> wallets;
}
