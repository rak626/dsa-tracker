package com.rakesh.dsa.tracker.schedule;

import com.rakesh.dsa.tracker.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupService backupService;

    // 12:00 AM daily
    @Scheduled(cron = "0 0 10 * * *")
    public void nightlyBackup() {
        backupService.backupNow("SCHEDULED_NIGHTLY");
    }

    // 1:00 PM daily
    @Scheduled(cron = "0 0 13 * * *")
    public void dayBackup() {
        backupService.backupNow("SCHEDULED_DAILY");
    }
}

