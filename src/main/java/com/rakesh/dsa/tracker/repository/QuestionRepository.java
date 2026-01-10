package com.rakesh.dsa.tracker.repository;

import com.rakesh.dsa.tracker.model.DifficultyType;
import com.rakesh.dsa.tracker.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("""
                SELECT DISTINCT q
                FROM Question q
                LEFT JOIN q.topics t
                LEFT JOIN q.patterns p
                WHERE
                    (
                        :search IS NULL OR
                        LOWER(q.problemName) LIKE :search OR
                        LOWER(q.problemLink) LIKE :search
                    )
                    AND (:difficulty IS NULL OR q.difficulty = :difficulty)
                    AND (:platform IS NULL OR q.platform = :platform)
                    AND (:topic IS NULL OR t.name = :topic)
                    AND (:pattern IS NULL OR p.name = :pattern)
                    AND q.lastAttemptedAt >= :fromInstant
            """)
    Page<Question> findAllWithFilters(
            @Param("search") String search,
            @Param("difficulty") DifficultyType difficulty,
            @Param("platform") String platform,
            @Param("topic") String topic,
            @Param("pattern") String pattern,
            @Param("fromInstant") Instant fromInstant,
            Pageable pageable
    );


}
