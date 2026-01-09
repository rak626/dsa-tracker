package com.rakesh.dsa.tracker.controller.ui;

import com.rakesh.dsa.tracker.model.dto.CreateQuestionRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        CreateQuestionRequest request = new CreateQuestionRequest();
        request.setSolved(true);
        request.setReviseCount(0);

        model.addAttribute("request", request);
        return QuestionViewConstants.INDEX;
    }
}

