package com.example.personservice.repository;

import com.example.personservice.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;

import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID>, RevisionRepository<Address, UUID, Long> {
}
