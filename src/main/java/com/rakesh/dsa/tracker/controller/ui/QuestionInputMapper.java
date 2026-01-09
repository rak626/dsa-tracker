package com.rakesh.dsa.tracker.controller.ui;

import com.rakesh.dsa.tracker.model.dto.CreateQuestionRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class QuestionInputMapper {

    public void map(
            CreateQuestionRequest request,
            String topicsInput,
            String patternsInput
    ) {
        request.setTopics(parse(topicsInput));
        request.setPatterns(parse(patternsInput));
    }

    private Set<String> parse(String input) {
        if (input == null || input.isBlank()) return Set.of();

        return Arrays.stream(input.replace("[", "")
                        .replace("]", "")
                        .split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }
}

