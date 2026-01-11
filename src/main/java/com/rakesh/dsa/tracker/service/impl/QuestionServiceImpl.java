package com.rakesh.dsa.tracker.service.impl;

import com.rakesh.dsa.tracker.model.*;
import com.rakesh.dsa.tracker.model.dto.CreateQuestionRequest;
import com.rakesh.dsa.tracker.repository.PatternRepository;
import com.rakesh.dsa.tracker.repository.QuestionRepository;
import com.rakesh.dsa.tracker.repository.QuestionSpecifications;
import com.rakesh.dsa.tracker.repository.TopicRepository;
import com.rakesh.dsa.tracker.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.rakesh.dsa.tracker.utils.QuestionServiceUtil.detectPlatform;
import static com.rakesh.dsa.tracker.utils.QuestionServiceUtil.detectProblemName;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final PatternRepository patternRepository;

    @Override
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
                .lastAttemptedAt(Instant.now())
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

    @Override
    public Page<Question> list(
            QuestionFilter filter,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                filter.filterDate() == null ? Sort.by(Sort.Direction.DESC, "videoId") : Sort.unsorted()
        );
        Specification<Question> spec = QuestionSpecifications.withFilters(filter);
        return questionRepository.findAll(spec, pageable);
    }


    @Override
    public String getRandomQuestion() {
        List<Question> all = questionRepository.findAll();
        if (all.isEmpty()) throw new NoSuchElementException("No questions available");
        return all.get(ThreadLocalRandom.current().nextInt(all.size())).getProblemLink();
    }

    @Override
    public Question incrementSolve(Long id) {
        Question q = questionRepository.findById(id).orElseThrow(() -> new RuntimeException("Question not found"));
        q.setSolveCount((q.getSolveCount() == null ? 1 : q.getSolveCount()) + 1);
        return questionRepository.save(q);
    }

    @Override
    public Question incrementRevise(Long id) {
        Question q = questionRepository.findById(id).orElseThrow();
        q.setReviseCount((q.getReviseCount() == null ? 1 : q.getReviseCount()) + 1);
        q.setLastAttemptedAt(Instant.now());
        return questionRepository.save(q);
    }

    @Override
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    @Override
    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    @Override
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

        if (req.getSolveCount() != null) {
            q.setSolveCount(req.getSolveCount());
            q.setLastAttemptedAt(Instant.now());
        }

        return questionRepository.save(q);
    }


    @Override
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Question not found"));
    }
}
