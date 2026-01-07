package com.rakesh.dsa.tracker.controller.ui;

import com.rakesh.dsa.tracker.model.Question;
import com.rakesh.dsa.tracker.model.dto.CreateQuestionRequest;
import com.rakesh.dsa.tracker.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class QuestionViewController {

    private final QuestionService questionService;

    // ================= HOME =================

    @GetMapping("/")
    public String home(Model model) {
        CreateQuestionRequest request = new CreateQuestionRequest();
        request.setSolved(true);   // ✅ default checked for UX
        request.setReviseCount(0); // optional UX improvement

        model.addAttribute("request", request);
        return "index";
    }

    // ================= CREATE =================

    @PostMapping("/add")
    public String add(
            @ModelAttribute("request") CreateQuestionRequest request,
            @RequestParam(required = false) String topicsInput,
            @RequestParam(required = false) String patternsInput,
            RedirectAttributes redirectAttributes
    ) {

        try {
            parseTopicAndPatternInput(request, topicsInput, patternsInput);
            questionService.create(request);
            redirectAttributes.addFlashAttribute("successMessage", "Question added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to add question: " + e.getMessage()
            );
        }

        return "redirect:/";
    }

    // ================= LIST =================

    @GetMapping("/questions")
    public String list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String pattern,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model
    ) {

        String platformNormalized =
                (platform != null && !platform.isBlank())
                        ? platform.toUpperCase()
                        : null;

        String normalizedSearch =
                (search == null || search.isBlank())
                        ? null
                        : search.trim(); // ← keep controller responsibility minimal

        var result = questionService.list(
                normalizedSearch,
                difficulty,
                platformNormalized,
                topic,
                pattern,
                page,
                size
        );

        model.addAttribute("page", result.getPageable().getPageNumber());
        model.addAttribute("size", size);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalElements", result.getTotalElements());

        model.addAttribute("questions", result.getContent());

        model.addAttribute("search", search);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("platform", platform);
        model.addAttribute("topic", topic);
        model.addAttribute("pattern", pattern);


        return "questions";
    }


    // ================= EDIT =================

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {

        Question q = questionService.getQuestionById(id);
        model.addAttribute("question", q);

        return "edit";
    }

    @PostMapping("/update/{id}")
    public String update(
            @PathVariable Long id,
            @ModelAttribute CreateQuestionRequest request,
            @RequestParam(required = false) String topicsInput,
            @RequestParam(required = false) String patternsInput,
            RedirectAttributes redirectAttributes
    ) {

        try {
            parseTopicAndPatternInput(request, topicsInput, patternsInput);
            questionService.updateQuestion(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Question updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to update question: " + e.getMessage()
            );
        }

        return "redirect:/questions";
    }

    // ================= ACTIONS =================

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        questionService.deleteQuestion(id);
        redirectAttributes.addFlashAttribute("successMessage", "Question deleted successfully.");
        return "redirect:/questions";
    }

    @GetMapping("/solve/{id}")
    public String toggleSolved(@PathVariable Long id) {
        questionService.toggleSolved(id);
        return "redirect:/questions";
    }

    @GetMapping("/revise/{id}")
    public String revise(@PathVariable Long id) {
        questionService.incrementRevise(id);
        return "redirect:/questions";
    }

    // ================= RANDOM =================

    @GetMapping("/random")
    public String randomRedirect() {
        return "redirect:" + questionService.getRandomQuestion();
    }

    // ================= HELPERS =================

    private void parseTopicAndPatternInput(
            CreateQuestionRequest request,
            String topicsInput,
            String patternsInput
    ) {

        if (topicsInput != null && !topicsInput.isBlank()) {
            Set<String> topics = Arrays.stream(
                            topicsInput.replace("[", "")
                                    .replace("]", "")
                                    .split(",")
                    )
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());

            request.setTopics(topics);
        }

        if (patternsInput != null && !patternsInput.isBlank()) {
            Set<String> patterns = Arrays.stream(
                            patternsInput.replace("[", "")
                                    .replace("]", "")
                                    .split(",")
                    )
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());

            request.setPatterns(patterns);
        }
    }
}
