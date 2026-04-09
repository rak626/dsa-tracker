package com.rakesh.dsa.tracker.config;

import com.rakesh.dsa.tracker.props.AppProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;
    private final AppProps appProps;

    @Override
    public Health health() {
        String token = appProps.getGithub().getToken();
        if (token == null || token.isBlank()) {
            return Health.unknown()
                    .withDetail("reason", "GitHub token not configured")
                    .build();
        }

        try {
            String url = String.format(
                    "https://api.github.com/repos/%s/%s",
                    appProps.getGithub().getUsername(),
                    appProps.getGithub().getRepo()
            );

            RequestEntity<Void> request = RequestEntity.get(java.net.URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .build();

            var response = restTemplate.exchange(request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                        .withDetail("repo", appProps.getGithub().getRepo())
                        .withDetail("branch", appProps.getGithub().getBranch())
                        .build();
            } else {
                return Health.down()
                        .withDetail("status", response.getStatusCode())
                        .withDetail("reason", "GitHub API returned non-200 status")
                        .build();
            }
        } catch (Exception e) {
            log.warn("GitHub health check failed", e);
            return Health.down()
                    .withDetail("reason", "Failed to connect to GitHub API")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
