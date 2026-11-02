package com.example.repository;

import com.example.entity.PaymentMethod;
import com.example.entity.PaymentType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MethodRepository extends JpaRepository<PaymentMethod, Integer> {

    @Query(// language=SQL
            """
                        SELECT pm
                        FROM PaymentMethod pm
                        JOIN pm.paymentMethodDefinition pmd
                        LEFT JOIN pm.paymentMethodRequiredFields
                        WHERE pmd.currencyCode = :currencyCode
                            AND (pmd.countryAlpha3Code =:countryCode OR pmd.isAllCountries = true)
                            AND (pm.isActive = true)
                            AND (pm.type = :type)
                    """)
    @EntityGraph(value = "withDefinitionAndRequiredField")
    List<PaymentMethod> findByCurrencyAndCountryAndType(String currencyCode, String countryCode, PaymentType type);
}
