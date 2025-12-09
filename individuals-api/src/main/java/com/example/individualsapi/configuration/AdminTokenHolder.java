package com.example.individualsapi.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Instant;

@Component
@NoArgsConstructor
public class AdminTokenHolder {

    @Getter
    private String accessToken;
    private Instant expiresAt;

    public boolean isExpired() {
        if (accessToken == null) {
            return true;
        }
        return this.expiresAt.isBefore(Instant.now());
    }

    public void update(String accessToken, Integer expiresIn) {
        Assert.notNull(accessToken, "Access token must not be null");
        Assert.notNull(expiresIn, "Expires in must not be null");
        this.accessToken = accessToken;
        this.expiresAt = Instant.now().plusSeconds(expiresIn);
    }
}
