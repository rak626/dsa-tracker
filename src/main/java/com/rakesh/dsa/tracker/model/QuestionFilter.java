package com.rakesh.dsa.tracker.model;

public record QuestionFilter(
        String search,
        DifficultyType difficulty,
        String platform,
        String topic,
        String pattern,
        String filterDate
) {
}
