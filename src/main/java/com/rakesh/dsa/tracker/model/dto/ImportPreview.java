package com.rakesh.dsa.tracker.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ImportPreview {
    private int totalQuestions;
    private int newQuestions;
    private int duplicateQuestions;
    private int newTopics;
    private int newPatterns;
    private List<String> newQuestionNames;
    private Map<String, String> newTopicNames;
    private Map<String, String> newPatternNames;
}
