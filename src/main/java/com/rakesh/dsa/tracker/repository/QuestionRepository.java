package com.rakesh.dsa.tracker.repository;

import com.rakesh.dsa.tracker.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QuestionRepository
        extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    boolean existsByProblemLink(String problemLink);
}
