package com.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Entity
@Table(schema = "payment", name = "merchants")
public class Merchant implements UserDetails {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "merchantIdGenerator", sequenceName = "merchant_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "merchantIdGenerator")
    private Integer id;

    @Column(name = "merchant_id", unique = true)
    private String merchantId;

    @OneToMany(mappedBy = "merchant", fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    @OneToMany(mappedBy = "merchant", fetch = FetchType.LAZY)
    private List<Payout> payouts;

    @Column(name = "secret_key")
    private String secretKey;

    @Column(name = "name")
    private String name;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Override
    public @Nullable String getPassword() {
        return secretKey;
    }

    @Override
    public String getUsername() {
        return merchantId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
