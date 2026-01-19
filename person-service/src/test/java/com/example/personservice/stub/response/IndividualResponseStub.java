package com.example.personservice.stub.response;

import com.example.person.dto.IndividualResponse;
import com.example.person.dto.IndividualResponseAddress;

import java.time.ZonedDateTime;
import java.util.UUID;

public class IndividualResponseStub {

    public static IndividualResponse individualResponse() {
        var individual = new IndividualResponse();
        individual.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        individual.setPhoneNumber("256");
        individual.setPassportNumber("123456");
        individual.setStatus("active");
        individual.setArchivedAt(ZonedDateTime.parse("2025-05-05T10:00:00Z"));
        individual.setVerifiedAt(ZonedDateTime.parse("2025-06-06T10:00:00Z"));
        individual.setFirstName("Alex");
        individual.setLastName("Alexov");
        individual.setSecretKey("alex secret");
        individual.setEmail("alex@alexov.mail");

        var address = new IndividualResponseAddress();
        address.setCountry("russia");
        address.setAddress("alex address");
        address.setCity("alex city");
        address.setZipCode("alex zip code");
        address.setState("alex state");

        individual.setAddress(address);

        return individual;
    }

    public static IndividualResponse individualResponse2() {
        var individual = new IndividualResponse();
        individual.setId(UUID.fromString("00000000-0000-0000-0000-000000000022"));
        individual.setPhoneNumber("256");
        individual.setPassportNumber("123456");
        individual.setStatus("active");
        individual.setArchivedAt(ZonedDateTime.parse("2025-05-05T10:00:00Z"));
        individual.setVerifiedAt(ZonedDateTime.parse("2025-06-06T10:00:00Z"));
        individual.setFirstName("Alexa");
        individual.setLastName("Alexov");
        individual.setSecretKey("alex secret");
        individual.setEmail("alex@alexov.mail");

        var address = new IndividualResponseAddress();
        address.setCountry("russia");
        address.setAddress("alexa address");
        address.setCity("alex city");
        address.setZipCode("alex zip code");
        address.setState("alex state");

        individual.setAddress(address);

        return individual;
    }
}
