package com.example.service;

import com.example.entity.Merchant;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

public interface MerchantService extends UserDetailsService {

    Optional<Merchant> getByLoginAndPassword(String login, String password);
}
