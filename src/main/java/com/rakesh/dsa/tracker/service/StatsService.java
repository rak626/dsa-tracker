package com.rakesh.dsa.tracker.service;

import com.rakesh.dsa.tracker.model.dto.StatsDTO;
import com.rakesh.dsa.tracker.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final QuestionRepository questionRepository;

    public StatsDTO getStats() {
        StatsDTO.StatsDTOBuilder builder = StatsDTO.builder();

        builder.totalQuestions(questionRepository.count());

        Instant now = Instant.now();
        builder.questionsToday(questionRepository.countAfter(startOfDay(now)));
        builder.questionsThisWeek(questionRepository.countAfter(startOfWeek(now)));
        builder.questionsThisMonth(questionRepository.countAfter(startOfMonth(now)));
        builder.questionsThisYear(questionRepository.countAfter(startOfYear(now)));

        builder.countByDifficulty(getCountByDifficulty());

        Long totalRevisions = questionRepository.totalRevisions();
        builder.totalRevisions(totalRevisions != null ? totalRevisions : 0);

        Double avgRevisions = questionRepository.averageRevisions();
        builder.averageRevisions(avgRevisions != null ? avgRevisions : 0.0);

        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        builder.dailyTrend(getDailyTrend(thirtyDaysAgo));

        builder.topTopics(getTopTopics());
        builder.topPatterns(getTopPatterns());

        return builder.build();
    }

    private Instant startOfDay(Instant instant) {
        return instant.atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
    }

    private Instant startOfWeek(Instant instant) {
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return date.minusDays(date.getDayOfWeek().getValue() - 1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
    }

    private Instant startOfMonth(Instant instant) {
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return date.withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
    }

    private Instant startOfYear(Instant instant) {
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return date.withDayOfYear(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
    }

    private Map<String, Long> getCountByDifficulty() {
        Map<String, Long> result = new HashMap<>();
        result.put("EASY", 0L);
        result.put("MEDIUM", 0L);
        result.put("HARD", 0L);
        
        List<Object[]> rows = questionRepository.countByDifficulty();
        
        for (Object[] row : rows) {
            String difficulty = row[0] != null ? row[0].toString() : "UNKNOWN";
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            result.put(difficulty, count);
        }
        
        return result;
    }

    private List<StatsDTO.DailyStats> getDailyTrend(Instant cutoff) {
        List<Object[]> rows = questionRepository.dailyTrend(cutoff);
        return rows.stream()
                .map(row -> StatsDTO.DailyStats.builder()
                        .date(((java.sql.Date) row[0]).toLocalDate())
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    private List<StatsDTO.TopicStats> getTopTopics() {
        List<Object[]> rows = questionRepository.topTopics();
        return rows.stream()
                .map(row -> StatsDTO.TopicStats.builder()
                        .name((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    private List<StatsDTO.PatternStats> getTopPatterns() {
        List<Object[]> rows = questionRepository.topPatterns();
        return rows.stream()
                .map(row -> StatsDTO.PatternStats.builder()
                        .name((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }
}
