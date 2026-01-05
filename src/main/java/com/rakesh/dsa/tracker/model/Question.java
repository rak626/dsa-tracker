package com.rakesh.dsa.tracker.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "questions")
public class Question {

    @Id
    private String id;

    private String videoId;

    private String problemName;
    private String problemLink;

    private String platform;      // LEETCODE / GFG / etc.
    private String difficulty;    // EASY / MEDIUM / HARD

    private List<String> topics;  // e.g. ["Array","DP"]
    private List<String> patterns; // e.g. ["Two Pointer","Sliding Window"]

    @Builder.Default
    private boolean solved = true;
    @Builder.Default
    private Integer reviseCount = 0;

    private Instant createdAt;
    private Instant updatedAt;
}
