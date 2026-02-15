package com.rakesh.dsa.tracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakesh.dsa.tracker.github.GitHubUploader;
import com.rakesh.dsa.tracker.model.Pattern;
import com.rakesh.dsa.tracker.model.Question;
import com.rakesh.dsa.tracker.model.Topic;
import com.rakesh.dsa.tracker.props.AppProps;
import com.rakesh.dsa.tracker.repository.PatternRepository;
import com.rakesh.dsa.tracker.repository.QuestionRepository;
import com.rakesh.dsa.tracker.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private static final DateTimeFormatter SNAPSHOT_TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    private static final String FILE_PREFIX = "questions_snapshot_";
    private static final String FILE_SUFFIX = ".json";

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final GitHubUploader gitHubUploader;
    private final AppProps props;
    private final TopicRepository topicRepo;
    private final PatternRepository patternRepo;

    public void backupNow(String reason) {
        if (!isBackupEnabled()) {
            log.debug("Backup skipped (disabled) | reason={}", reason);
            return;
        }

        Instant start = Instant.now();
        log.info("Backup started | reason={}", reason);
        try {
            List<Question> questions = questionRepository.findAll();
            int totalQuestions = questions.size();
            String fileName = buildSnapshotFileName();

            byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(questions);

            long snapshotSize = jsonBytes.length;

            log.info("Snapshot prepared | file={} | questions={} | size={} bytes", fileName, totalQuestions, snapshotSize);

            gitHubUploader.upload(fileName, jsonBytes);
            gitHubUploader.cleanupOldSnapshots(props.getBackup().getMaxFiles());

            long durationMs = Duration.between(start, Instant.now()).toMillis();

            log.info("Backup SUCCESS | file={} | duration={} ms", fileName, durationMs);

        } catch (Exception ex) {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            log.error("Backup FAILED | reason={} | duration={} ms", reason, durationMs, ex);
        }
    }

    @Transactional
    public String restore(MultipartFile file) {
        try {
            if (file.isEmpty()) throw new RuntimeException("File is empty");

            List<Question> incomingQuestions = objectMapper.readValue(file.getInputStream(), new TypeReference<>() {
            });

            // 1. Cache existing Topics/Patterns by NAME (safer than ID for restoration)
            Map<String, Topic> topicCache = topicRepo.findAll()
                    .stream()
                    .collect(
                            Collectors.toMap(
                                    t -> t.getName().toLowerCase(),
                                    t -> t,
                                    (a, b) -> a)
                    );

            Map<String, Pattern> patternCache = patternRepo.findAll().stream()
                    .collect(
                            Collectors.toMap(
                                    p -> p.getName().toLowerCase(),
                                    p -> p,
                                    (a, b) -> a)
                    );

            List<Question> finalQuestions = new ArrayList<>();

            for (Question jsonQ : incomingQuestions) {
                // 2. Avoid Duplicate Questions by checking the Unique Problem Link
                if (questionRepository.existsByProblemLink(jsonQ.getProblemLink())) {
                    continue; // Skip if already exists
                }

                // 3. Process Topics: Link to existing or create new
                List<Topic> managedTopics = jsonQ.getTopics().stream()
                        .map(t ->
                                topicCache.computeIfAbsent(t.getName().toLowerCase(),
                                        name -> {
                                            Topic newTopic = new Topic();
                                            newTopic.setName(t.getName());
                                            return topicRepo.saveAndFlush(newTopic);
                                        })).toList();

                // 4. Process Patterns: Link to existing or create new
                List<Pattern> managedPatterns = jsonQ.getPatterns().stream().map(p -> patternCache.computeIfAbsent(p.getName().toLowerCase(), name -> {
                    Pattern newPattern = new Pattern();
                    newPattern.setName(p.getName());
                    return patternRepo.saveAndFlush(newPattern);
                })).toList();

                // 5. Build NEW Question (ignore JSON ID to prevent Hibernate state issues)
                Question newQ = Question.builder()
                        .problemName(jsonQ.getProblemName())
                        .problemLink(jsonQ.getProblemLink())
                        .videoId(jsonQ.getVideoId())
                        .platform(jsonQ.getPlatform())
                        .difficulty(jsonQ.getDifficulty())
                        .solveCount(jsonQ.getSolveCount())
                        .reviseCount(jsonQ.getReviseCount())
                        .lastAttemptedAt(jsonQ.getLastAttemptedAt())
                        .topics(managedTopics)
                        .patterns(managedPatterns)
                        .build();

                finalQuestions.add(newQ);
            }

            questionRepository.saveAll(finalQuestions);

            log.info("Restore success | New records={}", finalQuestions.size());
            return "Restored " + finalQuestions.size() + " new questions successfully";

        } catch (Exception ex) {
            log.error("Restore failed", ex);
            throw new RuntimeException("Restore failed: " + ex.getMessage());
        }
    }


    private boolean isBackupEnabled() {
        return props.getBackup() != null && props.getBackup().isEnabled();
    }

    private String buildSnapshotFileName() {
        return FILE_PREFIX + LocalDateTime.now().format(SNAPSHOT_TS_FORMAT) + FILE_SUFFIX;
    }
}
