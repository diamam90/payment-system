package com.example.personservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(schema = "person", name = "countries")
public class Country {

    @Id
    @Column(name = "country_id")
    private Integer id;

    @Column(name = "name", length = 32)
    private String name;

    @Column(name = "alpha2", length = 2)
    private String alpha2;

    @Column(name = "alpha3", length = 3)
    private String alpha3;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "created")
    Instant created;

    @Column(name = "updated")
    Instant updated;
}
