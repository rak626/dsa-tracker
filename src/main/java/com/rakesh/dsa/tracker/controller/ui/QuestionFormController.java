package com.rakesh.dsa.tracker.controller.ui;

import com.rakesh.dsa.tracker.model.dto.CreateQuestionRequest;
import com.rakesh.dsa.tracker.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/questions")
public class QuestionFormController {

    private final QuestionService questionService;
    private final QuestionInputMapper inputMapper;

    @PostMapping
    public String add(
            @ModelAttribute CreateQuestionRequest request,
            @RequestParam(required = false) String topicsInput,
            @RequestParam(required = false) String patternsInput,
            RedirectAttributes ra
    ) {
        try {
            inputMapper.map(request, topicsInput, patternsInput);
            questionService.create(request);
            ra.addFlashAttribute("successMessage", "Question added successfully");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return QuestionViewConstants.REDIRECT_HOME;
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @ModelAttribute CreateQuestionRequest request,
            @RequestParam(required = false) String topicsInput,
            @RequestParam(required = false) String patternsInput,
            RedirectAttributes ra
    ) {
        inputMapper.map(request, topicsInput, patternsInput);
        questionService.updateQuestion(id, request);
        ra.addFlashAttribute("successMessage", "Question updated successfully");
        return QuestionViewConstants.REDIRECT_QUESTIONS;
    }
}
