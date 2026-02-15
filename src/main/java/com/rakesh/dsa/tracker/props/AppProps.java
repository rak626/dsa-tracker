package com.rakesh.dsa.tracker.props;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProps {

    private String name;

    private final Backup backup = new Backup();
    private final Database database = new Database();
    private final GitHub github = new GitHub();

    @Data
    public static class Backup {
        private boolean enabled;
        private String cron;
        private int maxFiles;
        private boolean schemaOnly;
        private String directory;
    }

    @Data
    public static class Database {
        private String host;
        private String port;
        private String name;
        private String user;
        private String password;
    }

    @Data
    public static class GitHub {
        private String username;
        private String branch;
        private String token;
        private String remoteUrl;
        private String repo;
    }
}
