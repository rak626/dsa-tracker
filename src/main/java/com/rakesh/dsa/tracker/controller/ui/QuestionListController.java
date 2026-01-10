package com.rakesh.dsa.tracker.controller.ui;

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
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String pattern,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model
    ) {

        var result = questionService.list(
                search,
                difficulty,
                normalize(platform),
                topic,
                pattern,
                dateFilter,
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

