package com.example.personservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Audited
@Table(schema = "person", name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "country_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Country country;

    @Column(name = "address", length = 128)
    private String address;

    @Column(name = "zip_code", length = 32)
    private String zipCode;

    @Column(name = "archived", nullable = false)
    private Instant archived;

    @Column(name = "city", length = 32)
    private String city;

    @Column(name = "state", length = 32)
    private String state;

    @CreationTimestamp
    @Column(name = "created")
    private Instant created;

    @UpdateTimestamp
    @Column(name = "updated")
    private Instant updated;

    @OneToMany(mappedBy = "address", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    private List<User> users = new ArrayList<>();

    public void addUser(User user) {
        this.users.add(user);
        user.setAddress(this);
    }

    public void removeUser(User user) {
        this.users.remove(user);
        user.setAddress(null);
    }
}

