package com.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Entity
@Table(schema = "payment", name = "providers")
public class Provider {

    @Id
    @GeneratedValue(generator = "providerIdGenerator")
    @SequenceGenerator(name = "providerIdGenerator", sequenceName = "providers_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "provider")
    private Set<PaymentMethod> paymentMethods;
}
