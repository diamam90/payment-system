package com.example.service;

import com.example.config.MerchantProperties;
import com.example.model.Merchant;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashMap;
import java.util.List;

public class MerchantService implements UserDetailsService {

    private final HashMap<String, Merchant> merchants = new HashMap<>();

    public MerchantService(MerchantProperties properties) {
        properties.getCredentialsList()
                .forEach(cred -> {
                            Merchant merchant = new Merchant(
                                    cred.getUsername(),
                                    cred.getPassword(),
                                    List.of(new SimpleGrantedAuthority(cred.getUsername())));
                            merchants.put(cred.getUsername(), merchant);
                        }
                );
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!merchants.containsKey(username)) {
            throw new UsernameNotFoundException("user '" + username + "' not found");
        }

        return merchants.get(username);
    }
}
