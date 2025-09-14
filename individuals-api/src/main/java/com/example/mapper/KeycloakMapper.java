package com.example.mapper;

import com.example.dto.TokenResponse;
import com.example.dto.UserInfoResponse;
import com.example.dto.keycloak.KeycloakTokenResponse;
import com.example.dto.keycloak.KeycloakUserInfoResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;

@Component
public class KeycloakMapper {

    public TokenResponse tokenResponse(KeycloakTokenResponse response) {
        var token = new TokenResponse();
        token.setAccessToken(response.accessToken());
        token.setRefreshToken(response.refreshToken());
        token.setTokenType(response.tokenType());
        token.setExpiresIn(response.expiresIn());
        return token;
    }

    public UserInfoResponse userInfoResponse(KeycloakUserInfoResponse response) {
        var info = new UserInfoResponse();
        info.setId(response.id());
        info.setEmail(response.username());
        info.setCreatedAt(Instant.ofEpochMilli(response.createdTimestamp()).atOffset(ZoneOffset.UTC));
        return info;
    }
}
