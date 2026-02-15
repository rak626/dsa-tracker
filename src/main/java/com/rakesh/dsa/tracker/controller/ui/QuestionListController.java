package com.rakesh.dsa.tracker.controller.ui;

import com.rakesh.dsa.tracker.model.QuestionFilter;
import com.rakesh.dsa.tracker.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/questions")
public class QuestionListController {

    private final QuestionService questionService;

    @GetMapping
    public String list(
            QuestionFilter questionFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {

        var result = questionService.list(
                questionFilter,
                page,
                size
        );

        model.addAttribute("questions", result.getContent());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("page", page);
        model.addAttribute("size", size);

        return QuestionViewConstants.QUESTIONS;
    }

    private String normalize(String platform) {
        return (platform == null || platform.isBlank())
                ? null
                : platform.toUpperCase();
    }
}

