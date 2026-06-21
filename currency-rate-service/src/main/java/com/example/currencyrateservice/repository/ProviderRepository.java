package com.example.currencyrateservice.repository;

import com.example.currencyrateservice.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProviderRepository extends JpaRepository<Provider, String> {

    List<Provider> findByProviderCodeIn(Collection<String> providerCodes);
}
