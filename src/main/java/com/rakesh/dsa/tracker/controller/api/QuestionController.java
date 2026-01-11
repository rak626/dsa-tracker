package com.rakesh.dsa.tracker.controller.api;

import com.rakesh.dsa.tracker.model.Question;
import com.rakesh.dsa.tracker.model.dto.CreateQuestionRequest;
import com.rakesh.dsa.tracker.service.impl.QuestionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionServiceImpl questionService;

    @PostMapping
    public ResponseEntity<?> add(@RequestBody CreateQuestionRequest request) {
        try {
            Question createdQuestion = questionService.create(request);
            return new ResponseEntity<>(createdQuestion, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllQuestions() {
        return new ResponseEntity<>(questionService.getAllQuestions(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok("Delete question with id: " + id);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<?> updateQuestion(@PathVariable Long id, @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.ok(questionService.updateQuestion(id, request));
    }

    @GetMapping("/random")
    public ResponseEntity<?> getRandomQuestion() {
        return ResponseEntity.ok(questionService.getRandomQuestion());
    }
}
