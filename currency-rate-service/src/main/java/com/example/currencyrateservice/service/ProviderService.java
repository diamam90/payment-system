package com.example.currencyrateservice.service;

import com.example.currencyrateservice.entity.Provider;

import java.util.Collection;
import java.util.List;

public interface ProviderService {

    List<Provider> saveAll(Collection<Provider> providers);

    List<Provider> getByProviderCodes(Collection<String> codes);

    List<Provider> findAll();

}
