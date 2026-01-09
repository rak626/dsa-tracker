package com.rakesh.dsa.tracker.controller;

import com.rakesh.dsa.tracker.model.Pattern;
import com.rakesh.dsa.tracker.model.Topic;
import com.rakesh.dsa.tracker.repository.PatternRepository;
import com.rakesh.dsa.tracker.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final TopicRepository topicRepo;
    private final PatternRepository patternRepo;

    @GetMapping("/topics")
    public List<String> topics(@RequestParam String q) {
        return topicRepo.findTop10ByNameContainingIgnoreCase(q)
                .stream()
                .map(Topic::getName)
                .toList();
    }

    @GetMapping("/patterns")
    public List<String> patterns(@RequestParam String q) {
        return patternRepo.findTop10ByNameContainingIgnoreCase(q)
                .stream()
                .map(Pattern::getName)
                .toList();
    }
}
