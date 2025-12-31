package com.gemstore. backend.repositories.listing.lookup;

import com.gemstore.backend.entities.listing.lookup.GemstoneType;
import org.springframework.data.jpa. repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GemstoneTypeRepository extends JpaRepository<GemstoneType, Integer> {

    List<GemstoneType> findByIsActiveTrue();

    Optional<GemstoneType> findByName(String name);

    List<GemstoneType> findByCategory(String category);
}