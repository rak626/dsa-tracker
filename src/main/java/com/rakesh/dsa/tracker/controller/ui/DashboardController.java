package com.rakesh.dsa.tracker.controller.ui;

import com.rakesh.dsa.tracker.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var stats = statsService.getStats();
        model.addAttribute("stats", stats);
        return "dashboard";
    }
}
