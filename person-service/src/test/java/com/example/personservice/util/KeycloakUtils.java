package com.example.personservice.util;

import com.example.personservice.config.AppContainers;
import dasniko.testcontainers.keycloak.ExtendableKeycloakContainer;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.core.Response;
import java.util.List;

public class KeycloakUtils {

    private static final String REALM = "payment";
    private static final String CLIENT_ID = "person-service";
    private static final String CLIENT_SECRET = "**********";
    private static final String ADMIN_ROLE = "payment_system_admin";

    public static String createKeycloakUser(UserRepresentation userRequest) {
        Keycloak adminClient = Keycloak.getInstance(AppContainers.keycloak.getAuthServerUrl(),
                ExtendableKeycloakContainer.MASTER_REALM,
                AppContainers.keycloak.getAdminUsername(),
                AppContainers.keycloak.getAdminPassword(),
                ExtendableKeycloakContainer.ADMIN_CLI_CLIENT);

        Response response = adminClient.realm(REALM).users().create(userRequest);
        String[] paths = response.getLocation().getPath().split("/");
        return paths[paths.length - 1];
    }

    public static void addAdminRoleToUser(String userId) {
        Keycloak adminClient = Keycloak.getInstance(AppContainers.keycloak.getAuthServerUrl(),
                ExtendableKeycloakContainer.MASTER_REALM,
                AppContainers.keycloak.getAdminUsername(),
                AppContainers.keycloak.getAdminPassword(),
                ExtendableKeycloakContainer.ADMIN_CLI_CLIENT);
        var adminRole = adminClient.realm(REALM).roles().list()
                .stream()
                .filter(role -> ADMIN_ROLE.equals(role.getName()))
                .findFirst().orElseThrow();
        adminClient.realm(REALM).users().get(userId).roles().realmLevel().add(List.of(adminRole));
    }

    public static AccessTokenResponse accessToken(UserRepresentation user) {
        Keycloak personClient = Keycloak.getInstance(AppContainers.keycloak.getAuthServerUrl(),
                REALM,
                user.getUsername(),
                user.getCredentials().getFirst().getValue(),
                CLIENT_ID,
                CLIENT_SECRET);

        return personClient.tokenManager().getAccessToken();
    }

    public static AccessTokenResponse adminToken() {
        Keycloak adminClient = Keycloak.getInstance(AppContainers.keycloak.getAuthServerUrl(),
                ExtendableKeycloakContainer.MASTER_REALM,
                AppContainers.keycloak.getAdminUsername(),
                AppContainers.keycloak.getAdminPassword(),
                ExtendableKeycloakContainer.ADMIN_CLI_CLIENT);

        return adminClient.tokenManager().getAccessToken();
    }

    public static void deleteKeycloakUser(String userId) {
        Keycloak adminClient = Keycloak.getInstance(AppContainers.keycloak.getAuthServerUrl(),
                ExtendableKeycloakContainer.MASTER_REALM,
                AppContainers.keycloak.getAdminUsername(),
                AppContainers.keycloak.getAdminPassword(),
                ExtendableKeycloakContainer.ADMIN_CLI_CLIENT);
        adminClient.realm(REALM).users().delete(userId);
    }
}
