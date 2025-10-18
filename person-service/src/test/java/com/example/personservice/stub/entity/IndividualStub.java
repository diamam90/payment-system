package com.example.personservice.stub.entity;

import com.example.personservice.entity.Address;
import com.example.personservice.entity.Country;
import com.example.personservice.entity.Individual;
import com.example.personservice.entity.User;

import java.time.Instant;
import java.util.UUID;

public class IndividualStub {

    public static Individual individual_1() {
        var individual = new Individual();
        individual.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        individual.setPhoneNumber("256");
        individual.setPassportNumber("123456");
        individual.setStatus("active");
        individual.setArchivedAt(Instant.parse("2025-05-05T10:00:00Z"));
        individual.setVerifiedAt(Instant.parse("2025-06-06T10:00:00Z"));

        var user = new User();
        user.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        user.setFirstName("Alex");
        user.setLastName("Alexov");
        user.setSecretKey("alex secret");
        user.setEmail("alex@alexov.mail");
        user.setCreated(Instant.parse("2025-05-05T10:00:00Z"));
        user.setUpdated(Instant.parse("2025-05-05T10:00:00Z"));

        var address = new Address();
        address.setAddress("alex address");
        address.setCreated(Instant.parse("2025-05-05T10:00:00Z"));
        address.setUpdated(Instant.parse("2025-05-05T10:00:00Z"));
        address.setCity("alex city");
        address.setZipCode("alex zip code");
        address.setState("alex state");

        var country = new Country();
        country.setId(1);
        country.setName("test country");
        country.setAlpha3("AAA");
        country.setAlpha2("AA");
        country.setStatus("active");

        address.setCountry(country);
        user.setAddress(address);
        individual.setUser(user);

        return individual;
    }

    public static Individual individualWithoutCountry() {
        var individual = new Individual();
        individual.setPhoneNumber("256");
        individual.setPassportNumber("123456");
        individual.setStatus("active");
        individual.setArchivedAt(Instant.parse("2025-05-05T10:00:00Z"));
        individual.setVerifiedAt(Instant.parse("2025-06-06T10:00:00Z"));

        var user = new User();
        user.setFirstName("Alex");
        user.setLastName("Alexov");
        user.setSecretKey("alex secret");
        user.setEmail("alex@alexov.mail");
        user.setCreated(Instant.parse("2025-05-05T10:00:00Z"));
        user.setUpdated(Instant.parse("2025-05-05T10:00:00Z"));

        var address = new Address();
        address.setAddress("alex address");
        address.setCreated(Instant.parse("2025-05-05T10:00:00Z"));
        address.setUpdated(Instant.parse("2025-05-05T10:00:00Z"));
        address.setCity("alex city");
        address.setZipCode("alex zip code");
        address.setState("alex state");
        address.setArchived(Instant.parse("2015-05-05T10:00:00Z"));

        user.setAddress(address);
        individual.setUser(user);

        return individual;
    }

    public static Individual firstNamov() {
        var individual = new Individual();
        individual.setPassportNumber("1331 4429");
        individual.setPhoneNumber("8800");
        individual.setVerifiedAt(Instant.parse("2025-05-05T18:00:00Z"));
        individual.setArchivedAt(Instant.parse("2025-06-06T10:00:00Z"));

        var country = new Country();
        country.setId(2);
        country.setAlpha2("AL");
        country.setAlpha3("ALB");
        country.setName("Albania");

        var address = new Address();
        address.setCity("VOLGOGRAD");
        address.setAddress("test address. 23");
        address.setZipCode("400400");
        address.setArchived(Instant.parse("2024-05-05T18:00:00Z"));
        address.setCountry(country);

        var user = new User();
        user.setFirstName("firstnamov");
        user.setEmail("email@email.email");
        user.setSecretKey("asdv232");
        user.setAddress(address);
        individual.setUser(user);

        return individual;
    }
}
