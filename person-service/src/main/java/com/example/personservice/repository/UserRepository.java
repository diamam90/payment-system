package com.example.personservice.repository;

import com.example.personservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, RevisionRepository<User, UUID, Long> {
}
