package com.rakesh.dsa.tracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;

@Configuration
@Slf4j
public class AppConfig {
    private static final int CONNECT_TIMEOUT_MS = 5000;    // 5 seconds
    private static final int READ_TIMEOUT_MS = 10000;      // 10 seconds
    private static final int WRITE_TIMEOUT_MS = 10000;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
