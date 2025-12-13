package com.gemstore. backend.repositories.listing.lookup;

import com.gemstore.backend.entities.listing.lookup.ClarityGrade;
import org.springframework.data.jpa. repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClarityGradeRepository extends JpaRepository<ClarityGrade, Integer> {

    List<ClarityGrade> findByIsActiveTrueOrderByRankAsc();

    Optional<ClarityGrade> findByName(String name);
}