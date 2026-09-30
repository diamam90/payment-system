package com.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(schema = "payment", name = "payment_methods")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "paymentMethodIdGenerator")
    @SequenceGenerator(name = "paymentMethodIdGenerator", sequenceName = "payment_methods_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    private Provider provider;

    @Column(name = "provider_id", updatable = false, insertable = false)
    private Integer providerId;

    @Column(name = "type")
    private String type;

    @Column(name = "name")
    private String name;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "provider_unique_id")
    private String providerUniqueId;

    @Column(name = "provider_method_type")
    private String providerMethodType;

    @Column(name = "logo")
    private String logo;

    @Column(name = "profile_type")
    private String profileType;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "payment_method_id")
    private PaymentMethodDefinition paymentMethodDefinition;

}
