package com.example.service.impl;

import com.example.entity.Merchant;
import com.example.repository.MerchantRepository;
import com.example.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Merchant> getByLoginAndPassword(String login, String password) {
        return merchantRepository.findByMerchantIdAndSecretKey(login, password);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return merchantRepository.findByMerchantId(username)
                .orElseThrow(() -> new UsernameNotFoundException("Merchant with username " + username + " not found"));
    }
}
