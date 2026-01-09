package com.rakesh.dsa.tracker.controller.ui;

import com.rakesh.dsa.tracker.model.Question;
import com.rakesh.dsa.tracker.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/questions")
public class QuestionActionController {

    private final QuestionService questionService;

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        questionService.deleteQuestion(id);
        ra.addFlashAttribute("successMessage", "Question deleted");
        return QuestionViewConstants.REDIRECT_QUESTIONS;
    }

    @GetMapping("/{id}/solve")
    public String toggleSolved(@PathVariable Long id) {
        questionService.toggleSolved(id);
        return QuestionViewConstants.REDIRECT_QUESTIONS;
    }

    @GetMapping("/{id}/revise")
    public String revise(@PathVariable Long id) {
        questionService.incrementRevise(id);
        return QuestionViewConstants.REDIRECT_QUESTIONS;
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {

        Question q = questionService.getQuestionById(id);
        model.addAttribute("question", q);

        return QuestionViewConstants.EDIT;
    }

    @GetMapping("/random")
    public String random() {
        return "redirect:" + questionService.getRandomQuestion();
    }
}
