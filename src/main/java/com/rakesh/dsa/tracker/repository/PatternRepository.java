package com.rakesh.dsa.tracker.repository;

import com.rakesh.dsa.tracker.model.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatternRepository extends JpaRepository<Pattern, Long> {
    Optional<Pattern> findByName(String name);

    List<Pattern> findTop10ByNameContainingIgnoreCase(String name);

}
