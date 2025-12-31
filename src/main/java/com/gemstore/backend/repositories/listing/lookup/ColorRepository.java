package com.gemstore.backend.repositories.listing.lookup;

import com.gemstore. backend.entities.listing.lookup. Color;
import org.springframework. data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColorRepository extends JpaRepository<Color, Integer> {

    List<Color> findByIsActiveTrue();

    Optional<Color> findByName(String name);
}