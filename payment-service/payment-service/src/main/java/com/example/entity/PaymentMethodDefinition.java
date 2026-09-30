package com.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "payment", name = "payment_method_definitions")
public class PaymentMethodDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "paymentMethodDefinitionIdGenerator")
    @SequenceGenerator(name = "paymentMethodDefinitionIdGenerator", sequenceName = "payment_method_definitions_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "payment_method_id")
    private Integer paymentMethodId;

    @OneToOne(mappedBy = "paymentMethodDefinition")
    private PaymentMethod paymentMethod;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "country_alpha3_code")
    private String countryAlpha3Code;

    @Column(name = "is_all_currencies")
    private Boolean isAllCurrencies;

    @Column(name = "is_all_countries")
    private Boolean isAllCountries;

    @Column(name = "is_priority")
    private Boolean isPriority;

    @Column(name = "is_active")
    private Boolean isActive;
}
