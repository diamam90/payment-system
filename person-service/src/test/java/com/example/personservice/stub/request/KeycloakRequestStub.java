package com.example.personservice.stub.request;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

public class KeycloakRequestStub {

    public static UserRepresentation user_1(UUID individualId) {
        var cred = new CredentialRepresentation();
        cred.setType(PASSWORD);
        cred.setValue("user1");

        var userRequest = new UserRepresentation();
        userRequest.setEmail("ya@mail.g");
        userRequest.setUsername("user1");
        userRequest.setCredentials(List.of(cred));
        userRequest.setEmailVerified(true);
        userRequest.setEnabled(true);
        if (individualId != null) {
            userRequest.setAttributes(Map.of("individualId", List.of(individualId.toString())));
        }

        return userRequest;
    }

    public static UserRepresentation user_2(UUID individualId, String email) {
        var cred = new CredentialRepresentation();
        cred.setType(PASSWORD);
        cred.setValue("user2");

        var userRequest = new UserRepresentation();
        userRequest.setEmail(email);
        userRequest.setUsername("user2");
        userRequest.setCredentials(List.of(cred));
        userRequest.setEmailVerified(true);
        userRequest.setEnabled(true);
        if (individualId != null) {
            userRequest.setAttributes(Map.of("individualId", List.of(individualId.toString())));
        }

        return userRequest;
    }

    public static UserRepresentation admin() {
        var cred = new CredentialRepresentation();
        cred.setType(PASSWORD);
        cred.setValue("user3");

        var userRequest = new UserRepresentation();
        userRequest.setUsername("user3");
        userRequest.setCredentials(List.of(cred));
        userRequest.setEmailVerified(true);
        userRequest.setEnabled(true);
        userRequest.setRealmRoles(List.of("ADMIN"));

        return userRequest;
    }
}
