package com.example.currencyrateservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(schema = "currency", name = "currency_rates")
public class CurrencyRate {

    @Id
    @SequenceGenerator(
            schema = "currency",
            name = "currencyRateIdGenerator",
            sequenceName = "currency_rates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "currencyRateIdGenerator")
    private Long id;

    @Column(name = "source_code")
    private String sourceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_code", referencedColumnName = "code", insertable = false, updatable = false)
    private Currency sourceCurrency;

    @Column(name = "destination_code")
    private String destinationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_code", referencedColumnName = "code", insertable = false, updatable = false)
    private Currency destinationCurrency;

    @Column(name = "rate_begin")
    private Instant rateBeginTime;

    @Column(name = "rate_end")
    private Instant rateEndTime;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "provider_code", insertable = false, updatable = false)
    private String providerCode;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "provider_code", referencedColumnName = "provider_code")
    private Provider provider;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at")
    private Instant modifiedAt;

    public void setDestinationCurrency(Currency currency) {
        if (currency != null) {
            this.destinationCode = currency.getCode();
        }
        this.destinationCurrency = currency;
    }

    public void setSourceCurrency(Currency currency) {
        if (currency != null) {
            this.sourceCode = currency.getCode();
        }
        this.sourceCurrency = currency;
    }

    public void setProvider(Provider provider) {
        if (provider != null) {
            this.providerCode = provider.getProviderCode();
        }
        this.provider = provider;
    }
}
