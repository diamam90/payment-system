package com.example.personservice.service.impl;

import com.example.person.dto.AddressRequest;
import com.example.person.dto.IndividualRequest;
import com.example.personservice.entity.*;
import com.example.personservice.exception.ObjectNotFoundException;
import com.example.personservice.mapper.IndividualMapper;
import com.example.personservice.repository.IndividualRepository;
import com.example.personservice.service.CountryService;
import com.example.personservice.service.IndividualService;
import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class IndividualServiceImpl implements IndividualService {

    private final CountryService countryService;
    private final IndividualRepository individualRepository;
    private final IndividualMapper individualMapper;

    @Override
    public Individual create(IndividualRequest request) {
        var country = Optional.ofNullable(request)
                .map(IndividualRequest::getAddress)
                .map(AddressRequest::getCountry)
                .map(countryService::getCountryByName)
                .orElse(null);

        var individual = individualMapper.create(request, country);
        var savedUser = individualRepository.save(individual);
        log.debug("User with id {} successfully saved", savedUser.getId());

        return savedUser;
    }

    @Override
    public Individual update(UUID id, IndividualRequest request) {
        var individual = individualRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(Individual.class, "id", id));
        update(individual, request);

        log.debug("User with id {} successfully updated", individual.getId());

        return individual;
    }

    @Override
    @Transactional(readOnly = true)
    public Individual findById(UUID id) {
        return individualRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException(Individual.class, "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Individual findByEmail(String email) {
        return individualRepository.findByEmail(email)
                .orElseThrow(() -> new ObjectNotFoundException(Individual.class, "email", email));
    }

    @Override
    public void softDelete(UUID id) {
        var individual = findById(id);
        individual.setStatus(Status.INACTIVE.getStatusCode());
        log.debug("User with id {} successfully soft deleted", id);
    }

    @Counted("hard_delete_individual")
    @Override
    public void hardDelete(UUID id) {
        individualRepository.deleteById(id);
        log.warn("User with id {} successfully hard deleted", id);
    }

    @Counted("activate_individual")
    @Override
    public void activateUser(UUID id) {
        var individual = findById(id);
        individual.setStatus(Status.ACTIVE.getStatusCode());
        log.warn("User with id {} successfully activated", id);
    }

    private void update(Individual individual, IndividualRequest request) {
        if (request == null) {
            return;
        }
        Country newCountry = Optional.ofNullable(request)
                .map(IndividualRequest::getAddress)
                .map(AddressRequest::getCountry)
                .map(countryService::getCountryByName)
                .orElse(null);

        User user;
        if (individual.getUser() != null) {
            user = individual.getUser();
        } else {
            user = new User();
            individual.setUser(user);
        }
        updateUser(user, request);

        if (request.getAddress() != null) {
            Address address = individual.getUser().getAddress();
            if (address == null) {
                address = new Address();
                user.setAddress(address);
            }
            updateAddress(address, request.getAddress(), newCountry);
        } else {
            user.setAddress(null);
        }

        updateIndividuals(individual, request);
    }

    private void updateAddress(Address address, AddressRequest request, Country country) {
        if (Objects.isNull(request)) {
            return;
        }
        address.setAddress(request.getAddress());
        address.setZipCode(request.getZipCode());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setArchived(request.getArchived().toInstant());
        address.setCountry(country);
    }

    private void updateUser(User user, IndividualRequest request) {
        if (Objects.isNull(request)) {
            return;
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setSecretKey(request.getSecretKey());
        user.setEmail(request.getEmail());
    }

    private void updateIndividuals(Individual individual, IndividualRequest request) {
        individual.setPassportNumber(request.getPassportNumber());
        individual.setPhoneNumber(request.getPhoneNumber());

        if (Objects.nonNull(request.getArchivedAt())) {
            individual.setArchivedAt(request.getArchivedAt().toInstant());
        } else {
            individual.setArchivedAt(null);
        }

        if (Objects.nonNull(request.getVerifiedAt())) {
            individual.setVerifiedAt(request.getVerifiedAt().toInstant());
        } else {
            individual.setVerifiedAt(null);
        }
    }
}
