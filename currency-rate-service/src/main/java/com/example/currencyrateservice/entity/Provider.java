package com.example.currencyrateservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(schema = "currency", name = "providers")
public class Provider implements Persistable<String> {

    @Id
    @Column(name = "provider_code", length = 3, unique = true)
    private String providerCode;

    @Column(name = "provider_name", length = 20, unique = true)
    private String providerName;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private Boolean active;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at")
    private Instant modifiedAt;

    @OneToMany(mappedBy = "provider", fetch = FetchType.LAZY)
    private List<CurrencyRate> currencyRates;

    @Nullable
    @Override
    public String getId() {
        return providerCode;
    }

    @Override
    public boolean isNew() {
        return modifiedAt == null;
    }
}
