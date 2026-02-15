package com.rakesh.dsa.tracker.github;

public class GitHubUploadException extends RuntimeException {

    public GitHubUploadException(String message) {
        super(message);
    }

    public GitHubUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
