package com.example.repository;

import com.example.entity.PaymentMethod;
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
                        WHERE (
                                (pmd.currencyCode = :currencyCode AND pmd.countryAlpha3Code =:countryCode)
                                OR (pmd.isAllCurrencies = true AND pmd.isAllCountries = true)
                            )
                            AND (pm.isActive = true)
                    """)
    @EntityGraph(value = "withDefinitionAndRequiredField")
    List<PaymentMethod> findByCurrencyCodeAndCountryCode(String currencyCode, String countryCode);
}
