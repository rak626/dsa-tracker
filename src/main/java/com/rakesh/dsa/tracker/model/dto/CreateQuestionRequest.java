package com.rakesh.dsa.tracker.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateQuestionRequest {

    private String videoId;       // optional

    private String problemLink;
    private String problemName;   // optional if blank you can still allow manual entry

    private String platform;
    private String difficulty;

    private List<String> topics;
    private List<String> patterns;

    private boolean isSolved;

    private Integer reviseCount;
}
