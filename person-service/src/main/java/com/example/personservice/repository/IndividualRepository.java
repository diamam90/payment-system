package com.example.personservice.repository;

import com.example.personservice.entity.Individual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndividualRepository extends JpaRepository<Individual, UUID>, RevisionRepository<Individual, UUID, Long> {

    @Query(
            """
                    SELECT i FROM Individual i
                    JOIN FETCH i.user u
                    JOIN FETCH u.address a
                    JOIN FETCH a.country
                    WHERE u.email = :email
                    """
    )
    Optional<Individual> findByEmail(String email);
}
