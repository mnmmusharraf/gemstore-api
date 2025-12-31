package com. gemstore.backend.repositories.listing.lookup;

import com.gemstore.backend.entities.listing. lookup.Origin;
import org. springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OriginRepository extends JpaRepository<Origin, Integer> {

    List<Origin> findByIsActiveTrue();

    Optional<Origin> findByName(String name);
}