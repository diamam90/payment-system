package com.example.personservice.stub.request;

import com.example.person.dto.AddressRequest;
import com.example.person.dto.IndividualRequest;

import java.time.ZonedDateTime;

public class IndividualRequestStub {

    public static IndividualRequest request_1() {
        var request = new IndividualRequest();
        request.setEmail("email228.email");
        request.setFirstName("first name");
        request.setLastName("last name");
        request.setPassportNumber("11111");
        request.setPhoneNumber("99999");
        request.setSecretKey("SECRET");
        request.setVerifiedAt(ZonedDateTime.parse("2025-05-05T12:00:26+03:00"));
        request.setArchivedAt(ZonedDateTime.parse("2025-05-05T11:00:26+04:00"));

        var address = address_1();
        request.setAddress(address);
        return request;
    }

    public static IndividualRequest request_2() {
        var request = new IndividualRequest();
        request.setEmail("ya@ya.ya");
        request.setFirstName("Ivan");
        request.setLastName("Ivanov");
        request.setPassportNumber("1818");
        request.setPhoneNumber("88002000600");
        request.setSecretKey("pssss");
        request.setVerifiedAt(ZonedDateTime.parse("3025-06-05T12:00:00+03:00"));
        request.setArchivedAt(ZonedDateTime.parse("3025-06-05T11:00:00+04:00"));

        // without country
        var address = address_2();
        request.setAddress(address);

        return request;
    }

    public static IndividualRequest update() {
        var request = new IndividualRequest();
        request.setFirstName("ffffirst");
        request.setLastName("llllast");
        request.setSecretKey("secret");
        request.setPassportNumber("1111");
        request.setPhoneNumber("121212");
        request.setEmail("ya@mail.g");
        request.setVerifiedAt(ZonedDateTime.parse("2022-02-02T02:02:02Z"));
        request.setArchivedAt(ZonedDateTime.parse("2022-02-02T03:03:03Z"));

        var addressRequest = updateAddress();

        request.setAddress(addressRequest);
        return request;
    }

    public static IndividualRequest create() {
        var request = new IndividualRequest();
        request.setFirstName("ffffirst");
        request.setLastName("llllast");
        request.setSecretKey("secret");
        request.setPassportNumber("1111");
        request.setPhoneNumber("121212");
        request.setEmail("ya@mail.g");
        request.setVerifiedAt(ZonedDateTime.parse("2022-02-02T02:02:02Z"));
        request.setArchivedAt(ZonedDateTime.parse("2022-02-02T03:03:03Z"));

        var addressRequest = createAddress();

        request.setAddress(addressRequest);
        return request;
    }

    private static AddressRequest address_1() {
        var address = new AddressRequest();
        address.setAddress("test address");
        address.setCity("test city");
        address.setCountry("Russia");
        address.setZipCode("228228");
        address.setState("test state");
        address.setArchived(ZonedDateTime.parse("2026-01-01T00:00:00+04:00"));

        return address;
    }

    private static AddressRequest address_2() {
        var address = new AddressRequest();
        address.setAddress("test address 2");
        address.setCity("test city 2");
        address.setZipCode("322");
        address.setState("test state 2");
        address.setArchived(ZonedDateTime.parse("2027-01-01T00:00:00+04:00"));

        return address;
    }

    private static AddressRequest address_3() {
        var address = new AddressRequest();
        address.setCountry("Hungary");
        address.setZipCode("111 111");
        address.setState("h state");
        address.setCity("h city");
        address.setAddress("h address");
        address.setArchived(ZonedDateTime.parse("2027-01-01T00:00:00+04:00"));

        return address;
    }

    private static AddressRequest createAddress() {
        var address = new AddressRequest();
        address.setCountry("Hungary");
        address.setZipCode("111 111");
        address.setState("h state");
        address.setCity("h city");
        address.setAddress("h address");
        address.setArchived(ZonedDateTime.parse("2022-02-02T03:03:03Z"));

        return address;
    }

    private static AddressRequest updateAddress() {
        var address = new AddressRequest();
        address.setCountry("Hungary");
        address.setZipCode("111 111");
        address.setState("h state");
        address.setCity("h city");
        address.setAddress("h address");
        address.setArchived(ZonedDateTime.parse("2023-03-03T03:03:03Z"));

        return address;
    }
}
