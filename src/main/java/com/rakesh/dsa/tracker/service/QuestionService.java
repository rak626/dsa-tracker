package com.rakesh.dsa.tracker.service;

import com.rakesh.dsa.tracker.model.Question;
import com.rakesh.dsa.tracker.model.dto.CreateQuestionRequest;
import com.rakesh.dsa.tracker.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

import static com.rakesh.dsa.tracker.utils.QuestionServiceUtil.detectPlatform;
import static com.rakesh.dsa.tracker.utils.QuestionServiceUtil.detectProblemName;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository repository;
    private final MongoTemplate mongoTemplate;

    public Question create(CreateQuestionRequest request) {

        if (request.getProblemLink() == null || !StringUtils.hasText(request.getProblemLink())) {
            throw new IllegalArgumentException("Problem link is required");
        }

        String platform = request.getPlatform() != null && StringUtils.hasText(request.getPlatform()) ? request.getPlatform() : detectPlatform(request.getProblemLink());

        String problemName = request.getProblemName() != null && StringUtils.hasText(request.getProblemName()) ? request.getProblemName() : detectProblemName(request.getProblemLink());

        Question q = Question.builder()
                .videoId(request.getVideoId())
                .problemName(problemName)
                .problemLink(request.getProblemLink())
                .platform(platform)
                .difficulty(request.getDifficulty())
                .topics(request.getTopics())
                .patterns(request.getPatterns())
                .solved(request.isSolved())
                .reviseCount(request.getReviseCount())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return repository.save(q);
    }

    public List<Question> getAllQuestions() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    public void deleteQuestion(String id) {
        repository.deleteById(id);
    }

    public Question updateQuestion(String id, CreateQuestionRequest req) {
        Question existingQuestion = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Question with id " + id + " not found"));

        if (req.getProblemLink() != null && StringUtils.hasText(req.getProblemLink())) {
            existingQuestion.setProblemLink(req.getProblemLink());
        }

        if (req.getProblemName() != null && StringUtils.hasText(req.getProblemName())) {
            existingQuestion.setProblemName(req.getProblemName());
        }

        if (req.getPlatform() != null && StringUtils.hasText(req.getPlatform())) {
            existingQuestion.setPlatform(req.getPlatform());
        }

        if (req.getDifficulty() != null && StringUtils.hasText(req.getDifficulty())) {
            existingQuestion.setDifficulty(req.getDifficulty());
        }

        if (req.getTopics() != null) {
            existingQuestion.setTopics(req.getTopics());
        }

        if (req.getPatterns() != null) {
            existingQuestion.setPatterns(req.getPatterns());
        }

        if(req.getVideoId() != null) {
            existingQuestion.setVideoId(req.getVideoId());
        }
        existingQuestion.setSolved(req.isSolved());

        if (req.getReviseCount() != null) {
            existingQuestion.setReviseCount(req.getReviseCount());
        }

        existingQuestion.setUpdatedAt(Instant.now());

        return repository.save(existingQuestion);
    }


    public String getRandomQuestion() {

        List<Question> allQuestions = repository.findAll();

        if (allQuestions.isEmpty()) {
            throw new NoSuchElementException("No questions available");
        }

        int index = ThreadLocalRandom.current().nextInt(allQuestions.size());
        return allQuestions.get(index).getProblemLink();
    }

    public Page<Question> list(String search, String difficulty, String platform, String topic, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Query query = new Query().with(pageable);

        // search filter
        if (search != null && !search.isBlank()) {
            query.addCriteria(new Criteria().orOperator(Criteria.where("problemName").regex(search, "i"), Criteria.where("problemLink").regex(search, "i")));
        }

        if (difficulty != null && !difficulty.isBlank()) {
            query.addCriteria(Criteria.where("difficulty").is(difficulty));
        }

        if (platform != null && !platform.isBlank()) {
            query.addCriteria(Criteria.where("platform").is(platform));
        }

        if (topic != null && !topic.isBlank()) {
            query.addCriteria(Criteria.where("topics").is(topic));
        }

        long total = mongoTemplate.count(query, Question.class);

        List<Question> results = mongoTemplate.find(query, Question.class);

        return new PageImpl<>(results, pageable, total);
    }


    public Question toggleSolved(String id) {
        Question q = repository.findById(id).orElseThrow();
        q.setSolved(!q.isSolved());
        q.setUpdatedAt(Instant.now());
        return repository.save(q);
    }

    public Question incrementRevise(String id) {
        Question q = repository.findById(id).orElseThrow();
        q.setReviseCount((q.getReviseCount() == null ? 0 : q.getReviseCount()) + 1);
        q.setUpdatedAt(Instant.now());
        return repository.save(q);
    }


}
