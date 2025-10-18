package com.example.individualsapi.mapper;

import com.example.individuals.dto.UserRequest;
import com.example.individuals.dto.UserResponse;
import com.example.individuals.dto.UserResponseAddress;
import com.example.person.dto.AddressRequest;
import com.example.person.dto.IndividualRequest;
import com.example.person.dto.IndividualResponse;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PersonMapper {

    public IndividualRequest individualRequest(UserRequest request) {
        var individual = new IndividualRequest();
        individual.setFirstName(request.getFirstName());
        individual.setLastName(request.getLastName());
        individual.setSecretKey(request.getSecretKey());
        individual.setEmail(request.getEmail());
        individual.setPassportNumber(request.getPassportNumber());
        individual.setPhoneNumber(request.getPhoneNumber());
        individual.setArchivedAt(request.getArchivedAt());
        individual.setVerifiedAt(request.getVerifiedAt());
        var addressRequest = request.getAddress();

        if (Objects.nonNull(addressRequest)) {
            var address = new AddressRequest();
            address.setCity(addressRequest.getCity());
            address.setCountry(addressRequest.getCountry());
            address.setState(addressRequest.getState());
            address.setZipCode(addressRequest.getZipCode());
            address.setArchived(addressRequest.getArchived());
            individual.setAddress(address);
        }
        return individual;
    }

    public UserResponse toUserResponse(IndividualResponse individual) {
        var user = new UserResponse();
        user.setId(individual.getId());
        user.setEmail(individual.getEmail());
        user.setSecretKey(individual.getSecretKey());
        user.setFirstName(individual.getFirstName());
        user.setLastName(individual.getLastName());
        user.setPassportNumber(individual.getPassportNumber());
        user.setPhoneNumber(individual.getPhoneNumber());
        user.setVerifiedAt(individual.getVerifiedAt());
        user.setArchivedAt(individual.getArchivedAt());
        user.setStatus(individual.getStatus());

        var individualAddress = individual.getAddress();
        if (individualAddress != null) {
            var address = new UserResponseAddress();
            address.setAddress(individualAddress.getAddress());
            address.setCity(individualAddress.getCity());
            address.setCountry(individualAddress.getCountry());
            address.setId(individualAddress.getId());
            address.setZipCode(individualAddress.getZipCode());
            address.setState(individualAddress.getState());
            user.setAddress(address);
        }
        return user;
    }
}
