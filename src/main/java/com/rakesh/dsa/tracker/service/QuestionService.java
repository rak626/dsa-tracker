package com.rakesh.dsa.tracker.service;

import com.rakesh.dsa.tracker.model.Question;
import com.rakesh.dsa.tracker.model.QuestionFilter;
import com.rakesh.dsa.tracker.model.dto.CreateQuestionRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface QuestionService {
    Question create(CreateQuestionRequest request);

    Page<Question> list(
            QuestionFilter questionFilter,
            int page,
            int size
    );

    String getRandomQuestion();

    Question incrementSolve(Long id);

    Question incrementRevise(Long id);

    List<Question> getAllQuestions();

    void deleteQuestion(Long id);

    Question updateQuestion(Long id, CreateQuestionRequest req);

    Question getQuestionById(Long id);
}
