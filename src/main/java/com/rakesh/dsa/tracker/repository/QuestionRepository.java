package com.rakesh.dsa.tracker.repository;

import com.rakesh.dsa.tracker.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface QuestionRepository
        extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    boolean existsByProblemLink(String problemLink);

    long count();

    @Query("SELECT COUNT(q) FROM Question q WHERE q.lastAttemptedAt >= :cutoff")
    long countAfter(@Param("cutoff") Instant cutoff);

    @Query("SELECT q.difficulty, COUNT(q) FROM Question q GROUP BY q.difficulty")
    List<Object[]> countByDifficulty();

    @Query("SELECT SUM(q.reviseCount) FROM Question q WHERE q.reviseCount > 0")
    Long totalRevisions();

    @Query("SELECT AVG(q.reviseCount) FROM Question q WHERE q.reviseCount > 0")
    Double averageRevisions();

    @Query(value = "SELECT CAST(last_attempted_at AS date), COUNT(*) FROM questions " +
           "WHERE last_attempted_at >= :cutoff GROUP BY CAST(last_attempted_at AS date) " +
           "ORDER BY CAST(last_attempted_at AS date)", nativeQuery = true)
    List<Object[]> dailyTrend(@Param("cutoff") Instant cutoff);

    @Query(value = "SELECT t.name, COUNT(qt.question_id) as cnt FROM questions q " +
           "JOIN question_topics qt ON q.id = qt.question_id " +
           "JOIN topics t ON qt.topic_id = t.id " +
           "GROUP BY t.name ORDER BY cnt DESC LIMIT 10", nativeQuery = true)
    List<Object[]> topTopics();

    @Query(value = "SELECT p.name, COUNT(qp.question_id) as cnt FROM questions q " +
           "JOIN question_patterns qp ON q.id = qp.question_id " +
           "JOIN patterns p ON qp.pattern_id = p.id " +
           "GROUP BY p.name ORDER BY cnt DESC LIMIT 10", nativeQuery = true)
    List<Object[]> topPatterns();
}
