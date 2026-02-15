package com.rakesh.dsa.tracker.github;

import lombok.Data;

@Data
public class GitHubContent {
    private String name;
    private String path;
    private String sha;
    private String type; // "file" or "dir"
}

