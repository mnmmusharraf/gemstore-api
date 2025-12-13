package com.gemstore.backend.repositories. listing.lookup;

import com. gemstore.backend.entities.listing.lookup.ColorQuality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype. Repository;

import java.util. List;
import java.util. Optional;

@Repository
public interface ColorQualityRepository extends JpaRepository<ColorQuality, Integer> {

    List<ColorQuality> findByIsActiveTrueOrderByRankAsc();

    Optional<ColorQuality> findByName(String name);
}