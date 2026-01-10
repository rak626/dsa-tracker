package com.rakesh.dsa.tracker.service;

import com.rakesh.dsa.tracker.model.DifficultyType;
import com.rakesh.dsa.tracker.model.Pattern;
import com.rakesh.dsa.tracker.model.Question;
import com.rakesh.dsa.tracker.model.Topic;
import com.rakesh.dsa.tracker.model.dto.CreateQuestionRequest;
import com.rakesh.dsa.tracker.model.dto.DateFilterEnum;
import com.rakesh.dsa.tracker.repository.PatternRepository;
import com.rakesh.dsa.tracker.repository.QuestionRepository;
import com.rakesh.dsa.tracker.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.rakesh.dsa.tracker.utils.QuestionServiceUtil.detectPlatform;
import static com.rakesh.dsa.tracker.utils.QuestionServiceUtil.detectProblemName;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final PatternRepository patternRepository;

    public Question create(CreateQuestionRequest request) {

        if (!StringUtils.hasText(request.getProblemLink())) {
            throw new IllegalArgumentException("Problem link is required");
        }

        String platform = StringUtils.hasText(request.getPlatform())
                ? request.getPlatform()
                : detectPlatform(request.getProblemLink());

        String problemName = StringUtils.hasText(request.getProblemName())
                ? request.getProblemName()
                : detectProblemName(request.getProblemLink());

        Question q = Question.builder()
                .videoId(request.getVideoId())
                .problemName(problemName)
                .problemLink(request.getProblemLink())
                .platform(platform)
                .difficulty(DifficultyType.valueOf(request.getDifficulty()))
                .solved(request.getSolved())
                .lastAttemptedAt(request.getSolved() != null && request.getSolved() ? Instant.now() : null)
                .reviseCount(request.getReviseCount())
                .topics(resolveTopics(request.getTopics()))
                .patterns(resolvePatterns(request.getPatterns()))
                .build();
        return questionRepository.save(q);
    }

    private List<Topic> resolveTopics(Set<String> names) {
        if (names == null) return List.of();

        List<Topic> result = new ArrayList<>();
        for (String name : names) {
            result.add(
                    topicRepository.findByName(name)
                            .orElseGet(() -> topicRepository.save(new Topic(null, name)))
            );
        }
        return result;
    }

    private List<Pattern> resolvePatterns(Set<String> names) {
        if (names == null) return List.of();

        List<Pattern> result = new ArrayList<>();
        for (String name : names) {
            result.add(
                    patternRepository.findByName(name)
                            .orElseGet(() -> patternRepository.save(new Pattern(null, name)))
            );
        }
        return result;
    }

    public Page<Question> list(
            String search,
            String difficulty,
            String platform,
            String topic,
            String pattern,
            String dateFilter,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        String normalizedSearch =
                (search == null || search.isBlank())
                        ? null
                        : "%" + search.trim().toLowerCase() + "%";

        DifficultyType difficultyEnum =
                (difficulty == null || difficulty.isBlank())
                        ? null
                        : DifficultyType.valueOf(difficulty);

        String normalizedPlatform =
                (platform == null || platform.isBlank())
                        ? null
                        : platform.toLowerCase();

        String normalizedTopic =
                (topic == null || topic.isBlank())
                        ? null
                        : topic;

        String normalizedPattern =
                (pattern == null || pattern.isBlank())
                        ? null
                        : pattern;

        Instant normalizeFromInstant =
                (dateFilter == null || dateFilter.isBlank())
                        ? Instant.EPOCH
                        : DateFilterEnum.fromString(dateFilter).cutoff();
        return questionRepository.findAllWithFilters(
                normalizedSearch,
                difficultyEnum,
                normalizedPlatform,
                normalizedTopic,
                normalizedPattern,
                normalizeFromInstant,
                pageable
        );
    }


    public String getRandomQuestion() {
        List<Question> all = questionRepository.findAll();
        if (all.isEmpty()) throw new NoSuchElementException("No questions available");
        return all.get(ThreadLocalRandom.current().nextInt(all.size())).getProblemLink();
    }

    public Question toggleSolved(Long id) {
        Question q = questionRepository.findById(id).orElseThrow(() -> new RuntimeException("Question not found"));
        q.setSolved(!q.isSolved());
        if (q.isSolved()) {
            q.setLastAttemptedAt(Instant.now());
        }
        return questionRepository.save(q);
    }

    public Question incrementRevise(Long id) {
        Question q = questionRepository.findById(id).orElseThrow();
        q.setReviseCount((q.getReviseCount() == null ? 0 : q.getReviseCount()) + 1);
        q.setLastAttemptedAt(Instant.now());
        return questionRepository.save(q);
    }

    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    public Question updateQuestion(Long id, CreateQuestionRequest req) {

        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        if (StringUtils.hasText(req.getProblemLink())) {
            q.setProblemLink(req.getProblemLink());
        }

        if (StringUtils.hasText(req.getProblemName())) {
            q.setProblemName(req.getProblemName());
        }

        if (StringUtils.hasText(req.getPlatform())) {
            q.setPlatform(req.getPlatform());
        }

        if (StringUtils.hasText(req.getDifficulty())) {
            q.setDifficulty(DifficultyType.valueOf(req.getDifficulty()));
        }

        if (req.getVideoId() != null) {
            q.setVideoId(req.getVideoId());
        }

        if (req.getReviseCount() != null) {
            q.setReviseCount(req.getReviseCount());
            q.setLastAttemptedAt(Instant.now());
        }

        if (req.getTopics() != null) {
            q.setTopics(resolveTopics(req.getTopics()));
        }

        if (req.getPatterns() != null) {
            q.setPatterns(resolvePatterns(req.getPatterns()));
        }

        if (req.getSolved() != null) {
            q.setSolved(req.getSolved());
        }

        return questionRepository.save(q);
    }


    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Question not found"));
    }
}
