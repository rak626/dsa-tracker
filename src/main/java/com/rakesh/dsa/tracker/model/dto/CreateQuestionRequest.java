package com.rakesh.dsa.tracker.model.dto;

import lombok.Data;

import java.util.Set;

@Data
public class CreateQuestionRequest {

    private String videoId;

    private String problemLink;
    private String problemName;

    private String platform;
    private String difficulty;

    // Names only — NOT entities
    private Set<String> topics;
    private Set<String> patterns;

    private Integer solveCount;

    private Integer reviseCount;
}
