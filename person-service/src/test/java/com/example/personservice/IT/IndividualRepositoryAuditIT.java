package com.example.personservice.IT;

import com.example.personservice.config.AppContainers;
import com.example.personservice.entity.Address;
import com.example.personservice.entity.Individual;
import com.example.personservice.entity.User;
import com.example.personservice.repository.AddressRepository;
import com.example.personservice.repository.IndividualRepository;
import com.example.personservice.repository.UserRepository;
import com.example.personservice.stub.entity.IndividualStub;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.data.history.Revision;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ImportTestcontainers(AppContainers.class)
@Testcontainers(disabledWithoutDocker = true)
public class IndividualRepositoryAuditIT {

    @Autowired
    IndividualRepository individualRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AddressRepository addressRepository;

    @Test
    void auditingTest() {
        Individual individual = IndividualStub.individualWithoutCountry();
        Individual save = individualRepository.save(individual);
        Optional<Revision<Long, User>> userRevision = userRepository.findLastChangeRevision(save.getUser().getId());
        Optional<Revision<Long, Individual>> individualRevision = individualRepository.findLastChangeRevision(save.getId());
        Optional<Revision<Long, Address>> addressRevision = addressRepository.findLastChangeRevision(save.getUser().getAddress().getId());

        assertTrue(individualRevision.isPresent());
        assertTrue(userRevision.isPresent());
        assertTrue(addressRevision.isPresent());
    }
}
