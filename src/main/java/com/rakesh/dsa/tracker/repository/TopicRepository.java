package com.rakesh.dsa.tracker.repository;

import com.rakesh.dsa.tracker.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    Optional<Topic> findByName(String name);

    List<Topic> findTop10ByNameContainingIgnoreCase(String name);

}
