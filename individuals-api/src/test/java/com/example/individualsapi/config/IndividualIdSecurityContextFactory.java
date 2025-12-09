package com.example.individualsapi.config;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class IndividualIdSecurityContextFactory implements WithSecurityContextFactory<WithIndividualIdUser> {

    @Override
    public SecurityContext createSecurityContext(WithIndividualIdUser annotation) {
        Jwt jwt = Jwt.withTokenValue("mockTokenValue")
                .claim("individual_id", annotation.value())
                .header("testHeader", "test")
                .build();
        JwtAuthenticationToken jwtToken = new JwtAuthenticationToken(jwt, null);
        SecurityContext context = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
        context.setAuthentication(jwtToken);
        return context;
    }
}