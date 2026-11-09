package com.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(schema = "payment", name = "payment_methods")
@NamedEntityGraph(name = "withDefinitionAndRequiredField", attributeNodes = {
        @NamedAttributeNode("paymentMethodDefinition"),
        @NamedAttributeNode("paymentMethodRequiredFields")
})
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "paymentMethodIdGenerator")
    @SequenceGenerator(name = "paymentMethodIdGenerator", sequenceName = "payment_methods_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @Column(name = "provider_id", updatable = false, insertable = false)
    private Integer providerId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "type")
    private PaymentType type;

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
    @Column(name = "modified_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "paymentMethod")
    private PaymentMethodDefinition paymentMethodDefinition;

    @OneToMany(mappedBy = "paymentMethod", fetch = FetchType.LAZY)
    private List<PaymentMethodRequiredField> paymentMethodRequiredFields;
}
