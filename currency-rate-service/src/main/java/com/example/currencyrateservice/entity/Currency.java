package com.example.currencyrateservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(schema = "currency", name = "currencies")
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "currencyIdGenerator")
    @SequenceGenerator(
            schema = "currency",
            name = "currencyIdGenerator",
            sequenceName = "currencies_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "code", unique = true)
    private String code;

    @Column(name = "iso_code")
    private Integer isoCode;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "symbol")
    private String symbol;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at")
    private Instant modifiedAt;

    @OneToMany(mappedBy = "sourceCurrency")
    private List<CurrencyRate> sourceRates;

    @OneToMany(mappedBy = "destinationCurrency")
    private List<CurrencyRate> destinationRates;
}
