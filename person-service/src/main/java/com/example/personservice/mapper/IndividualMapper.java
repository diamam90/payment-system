package com.example.personservice.mapper;

import com.example.personservice.entity.Address;
import com.example.personservice.entity.Country;
import com.example.personservice.entity.Individual;
import com.example.personservice.entity.User;
import com.example.person.dto.IndividualRequest;
import com.example.person.dto.IndividualResponse;
import com.example.person.dto.IndividualResponseAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IndividualMapper {

    public IndividualResponse toDto(Individual individual) {
        var dto = new IndividualResponse();
        dto.setId(individual.getId());

        if (Objects.nonNull(individual.getUser())) {
            var user = individual.getUser();
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setSecretKey(user.getSecretKey());

            if (Objects.nonNull(user.getAddress())) {
                var address = user.getAddress();
                var addressDto = new IndividualResponseAddress();
                addressDto.setAddress(address.getAddress());
                addressDto.setCity(address.getCity());
                addressDto.setCountry(address.getCountry().getName());
                addressDto.setState(address.getState());
                addressDto.setZipCode(address.getZipCode());
                dto.setAddress(addressDto);
            }
        }

        dto.setPassportNumber(individual.getPassportNumber());
        dto.setPhoneNumber(individual.getPhoneNumber());
        dto.setVerifiedAt(individual.getVerifiedAt().atZone(ZoneId.systemDefault()));
        dto.setArchivedAt(individual.getArchivedAt().atZone(ZoneId.systemDefault()));

        return dto;
    }

    public Individual create(IndividualRequest request, Country country) {
        var individual = new Individual();

        individual.setPassportNumber(request.getPassportNumber());
        individual.setPhoneNumber(request.getPhoneNumber());
        individual.setStatus("active");
        Optional.ofNullable(request.getArchivedAt())
                .ifPresent(time -> individual.setArchivedAt(time.toInstant()));
        Optional.ofNullable(request.getVerifiedAt())
                .ifPresent(time -> individual.setVerifiedAt(time.toInstant()));

        var user = new User();
        user.setEmail(request.getEmail());
        // TODO user.setFilled();
        user.setSecretKey(request.getSecretKey());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        var addressDto = request.getAddress();
        if (Objects.nonNull(addressDto)) {
            var address = new Address();
            address.setAddress(addressDto.getAddress());
            address.setArchived(addressDto.getArchived().toInstant());
            address.setCity(addressDto.getCity());
            address.setZipCode(addressDto.getZipCode());
            address.setState(addressDto.getState());
            address.setCountry(country);
            user.setAddress(address);
        }

        individual.setUser(user);

        return individual;
    }
}
