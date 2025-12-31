package com.gemstore.backend.repositories.listing.lookup;

import com.gemstore. backend.entities.listing.lookup. Treatment;
import org.springframework. data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Integer> {

    List<Treatment> findByIsActiveTrue();

    Optional<Treatment> findByName(String name);
}