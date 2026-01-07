package com.rakesh.dsa.tracker.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String videoId;

    @Column(nullable = false)
    private String problemName;

    @Column(nullable = false, unique = true)
    private String problemLink;

    private String platform;

    @Enumerated(EnumType.STRING)
    private DifficultyType difficulty;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "question_topics",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    private Set<Topic> topics = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "question_patterns",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "pattern_id")
    )
    private Set<Pattern> patterns = new HashSet<>();

    @Builder.Default
    private boolean solved = true;

    @Builder.Default
    private Integer reviseCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Transient
    public String getTopicNames() {
        return topics.stream()
                .map(Topic::getName)
                .collect(Collectors.joining(", "));
    }

    @Transient
    public String getPatternNames() {
        return patterns.stream()
                .map(Pattern::getName)
                .collect(Collectors.joining(", "));
    }

    @Transient
    public String getDifficultyLabel() {
        return difficulty == null ? "" : difficulty.name().toLowerCase();
    }


}
