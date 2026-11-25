package com.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(schema = "payment", name = "payment_method_required_fields")
public class PaymentMethodRequiredField {

    @Id
    @Column(name = "uid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uid;

    @ManyToOne
    @JoinColumn(name = "payment_method_id")
    PaymentMethod paymentMethod;

    @Column(name = "payment_method_id",insertable = false,updatable = false)
    Long paymentMethodId;

    @Column(name = "payment_type")
    String paymentType;

    @Column(name = "country_alpha3_code ")
    String countryAlpha3Code;

    @Column(name = "name")
    String name;

    @Column(name = "data_type")
    String dataType;

    @Column(name = "validation_type")
    String validationType;

    @Column(name = "validation_rule")
    String validationRule;

    @Column(name = "default_value")
    String defaultValue;

    @Column(name = "values_options")
    String valuesOptions;

    @Column(name = "description")
    String description;

    @Column(name = "placeholder")
    String placeholder;

    @Column(name = "representation_name")
    String representationName;

    @Column(name = "language")
    String language;

    @Column(name = "is_active")
    Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at")
    LocalDateTime updatedAt;
}
