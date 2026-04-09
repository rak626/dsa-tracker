package com.rakesh.dsa.tracker.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class StatsDTO {
    private long totalQuestions;
    private long questionsToday;
    private long questionsThisWeek;
    private long questionsThisMonth;
    private long questionsThisYear;
    
    private Map<String, Long> countByDifficulty;
    private long totalRevisions;
    private double averageRevisions;
    
    private List<DailyStats> dailyTrend;
    private List<TopicStats> topTopics;
    private List<PatternStats> topPatterns;
    
    @Data
    @Builder
    public static class DailyStats {
        private LocalDate date;
        private long count;
    }
    
    @Data
    @Builder
    public static class TopicStats {
        private String name;
        private long count;
    }
    
    @Data
    @Builder
    public static class PatternStats {
        private String name;
        private long count;
    }
}
