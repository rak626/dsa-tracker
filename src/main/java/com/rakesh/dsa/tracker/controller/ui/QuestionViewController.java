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

@Controller
@RequiredArgsConstructor
public class QuestionViewController {

    private final QuestionService questionService;


    // ================= HOME (ADD FORM) =================

    @GetMapping("/")
    public String home(Model model) {

        model.addAttribute("request", new CreateQuestionRequest());

        return "index"; // <-- Add page
    }


    // ================= CREATE =================

    @PostMapping("/add")
    public String add(@ModelAttribute("request") CreateQuestionRequest request, @RequestParam(required = false) String topicsInput, @RequestParam(required = false) String patternsInput, RedirectAttributes redirectAttributes) {

        try {
            if (topicsInput != null && !topicsInput.isBlank()) {
                request.setTopics(Arrays.stream(topicsInput.replace("[", "").replace("]", "").split(",")).map(String::trim).toList());
            }

            if (patternsInput != null && !patternsInput.isBlank()) {
                request.setPatterns(Arrays.stream(patternsInput.replace("[", "").replace("]", "").split(",")).map(String::trim).toList());
            }

            questionService.create(request);

            redirectAttributes.addFlashAttribute("successMessage", "Question added successfully.");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add question: " + e.getMessage());
        }

        return "redirect:/";
    }


    // ================= LIST PAGE =================

    @GetMapping("/questions")
    public String list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "0") int page, Model model
    ) {

        String platformModified = null;
        if ((platform != null && platform.isBlank())) {
            platformModified = platform.toUpperCase();
        }

        var result = questionService.list(search, difficulty, platformModified, topic, page, 10);

        model.addAttribute("page", result);
        model.addAttribute("questions", result.getContent());

        model.addAttribute("search", search);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("platform", platform);
        model.addAttribute("topic", topic);

        return "questions"; // <-- New page
    }


    // ================= EDIT =================

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model model) {

        Question q = questionService.getAllQuestions().stream().filter(x -> x.getId().equals(id)).findFirst().orElseThrow();

        model.addAttribute("question", q);
        return "edit";
    }


    @PostMapping("/update/{id}")
    public String update(@PathVariable String id, @ModelAttribute CreateQuestionRequest request, @RequestParam(required = false) String topicsInput, @RequestParam(required = false) String patternsInput, RedirectAttributes redirectAttributes) {

        try {

            if (topicsInput != null && !topicsInput.isBlank()) {
                request.setTopics(Arrays.stream(topicsInput.replace("[", "").replace("]", "").split(",")).map(String::trim).toList());
            }

            if (patternsInput != null && !patternsInput.isBlank()) {
                request.setPatterns(Arrays.stream(patternsInput.replace("[", "").replace("]", "").split(",")).map(String::trim).toList());
            }

            questionService.updateQuestion(id, request);

            redirectAttributes.addFlashAttribute("successMessage", "Question updated successfully.");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update question: " + e.getMessage());
        }

        return "redirect:/questions";
    }


    // ================= ACTION BUTTONS =================

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {

        questionService.deleteQuestion(id);

        redirectAttributes.addFlashAttribute("successMessage", "Question deleted successfully.");

        return "redirect:/questions";
    }


    @GetMapping("/solve/{id}")
    public String toggleSolved(@PathVariable String id) {
        questionService.toggleSolved(id);
        return "redirect:/questions";
    }


    @GetMapping("/revise/{id}")
    public String revise(@PathVariable String id) {
        questionService.incrementRevise(id);
        return "redirect:/questions";
    }


    // ================= RANDOM =================

    @GetMapping("/random")
    public String randomRedirect() {
        return "redirect:" + questionService.getRandomQuestion();
    }

}
