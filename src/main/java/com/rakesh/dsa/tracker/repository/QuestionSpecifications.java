package com.rakesh.dsa.tracker.repository;

import com.rakesh.dsa.tracker.model.*;
import com.rakesh.dsa.tracker.model.dto.DateFilterEnum;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QuestionSpecifications {

    public static Specification<Question> withFilters(QuestionFilter filter) {
        return (root, query, cb) -> {

            Objects.requireNonNull(query).distinct(true); // replaces DISTINCT in JPQL

            List<Predicate> predicates = new ArrayList<>();

            // ---- Search ----
            if (filter.search() != null && !filter.search().isBlank()) {
                String like = "%" + filter.search().trim().toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("problemName")), like),
                                cb.like(cb.lower(root.get("problemLink")), like)
                        )
                );
            }

            // ---- Difficulty ----
            if (filter.difficulty() != null) {
                predicates.add(
                        cb.equal(root.get("difficulty"), filter.difficulty())
                );
            }

            // ---- Platform ----
            if (filter.platform() != null && !filter.platform().isBlank()) {
                predicates.add(
                        cb.equal(root.get("platform"), filter.platform().toLowerCase())
                );
            }

            // ---- Topic ----
            if (filter.topic() != null && !filter.topic().isBlank()) {
                Join<Question, Topic> topicJoin =
                        root.join("topics", JoinType.LEFT);

                predicates.add(
                        cb.equal(topicJoin.get("name"), filter.topic())
                );
            }

            // ---- Pattern ----
            if (filter.pattern() != null && !filter.pattern().isBlank()) {
                Join<Question, Pattern> patternJoin =
                        root.join("patterns", JoinType.LEFT);

                predicates.add(
                        cb.equal(patternJoin.get("name"), filter.pattern())
                );
            }

            // ---- Time filter ----
            if (filter.filterDate() != null && !filter.filterDate().isBlank()) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("lastAttemptedAt"),
                                DateFilterEnum.valueOf(filter.filterDate()).cutoff()
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

