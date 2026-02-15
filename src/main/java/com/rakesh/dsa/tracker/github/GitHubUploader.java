package com.rakesh.dsa.tracker.github;

import com.rakesh.dsa.tracker.props.AppProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubUploader {

    private static final String SNAPSHOT_DIR = "snapshots/";

    private final AppProps props;
    private final RestTemplate restTemplate;

    public void upload(String fileName, byte[] content) {
        String fullPath = SNAPSHOT_DIR + fileName;
        String url = buildContentsUrl(fullPath);
        log.info("GitHub upload start | file={} | size={} bytes", fileName, content.length);

        try {
            HttpEntity<Map<String, Object>> request = buildUploadRequest(fileName, content);
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            validateResponse(response, "Upload failed");
            log.info("GitHub upload success | file={}", fileName);
        } catch (Exception ex) {
            log.error("GitHub upload failed | file={}", fileName, ex);
            throw ex;
        }
    }

    public void cleanupOldSnapshots(int maxSnapshots) {

        log.info("Cleanup start | maxSnapshots={}", maxSnapshots);

        List<GitHubContent> snapshots = fetchSnapshots();

        if (snapshots.size() <= maxSnapshots) {
            log.info("Cleanup skipped | existing={}", snapshots.size());
            return;
        }

        List<GitHubContent> toDelete =
                snapshots.subList(maxSnapshots, snapshots.size());

        log.info("Deleting {} old snapshots", toDelete.size());

        toDelete.forEach(this::deleteFile);

        log.info("Cleanup finished");
    }

    // ================= FETCH =================

    private List<GitHubContent> fetchSnapshots() {

        String url = buildContentsUrl("snapshots");

        HttpEntity<Void> request = new HttpEntity<>(buildAuthHeaders());

        ResponseEntity<GitHubContent[]> response =
                restTemplate.exchange(url, HttpMethod.GET, request, GitHubContent[].class);

        validateResponse(response, "Fetch snapshots failed");

        GitHubContent[] contents = response.getBody();
        if (contents == null) return List.of();

        return Arrays.stream(contents)
                .filter(c -> "file".equalsIgnoreCase(c.getType()))
                .filter(c -> c.getName().startsWith("questions_snapshot_"))
                .sorted(Comparator.comparing(GitHubContent::getName).reversed())
                .toList();
    }


    private void deleteFile(GitHubContent file) {

        String url = buildContentsUrl(file.getPath());

        Map<String, Object> body = Map.of(
                "message", "Delete old snapshot: " + file.getName(),
                "sha", file.getSha(),
                "branch", props.getGithub().getBranch()
        );

        HttpHeaders headers = buildAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

        validateResponse(response, "Delete failed");

        log.info("Deleted snapshot | {}", file.getName());
    }

    private HttpEntity<Map<String, Object>> buildUploadRequest(String fileName, byte[] content) {

        Map<String, Object> body = Map.of(
                "message", "Backup snapshot: " + fileName,
                "content", Base64.getEncoder().encodeToString(content),
                "branch", props.getGithub().getBranch()
        );

        HttpHeaders headers = buildAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(props.getGithub().getToken());
        headers.set("User-Agent", "dsa-tracker-backup-service");
        headers.setAccept(List.of(MediaType.parseMediaType("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        return headers;
    }

    private void validateResponse(ResponseEntity<?> response, String msg) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(msg + " | status=" + response.getStatusCode());
        }
    }

    private String buildContentsUrl(String path) {
        return String.format(
                "https://api.github.com/repos/%s/%s/contents/%s",
                props.getGithub().getUsername(),
                props.getGithub().getRepo(),
                path
        );
    }
}
