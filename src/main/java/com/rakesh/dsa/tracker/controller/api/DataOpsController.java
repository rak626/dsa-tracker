package com.rakesh.dsa.tracker.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakesh.dsa.tracker.model.Question;
import com.rakesh.dsa.tracker.repository.QuestionRepository;
import com.rakesh.dsa.tracker.service.BackupService;
import com.rakesh.dsa.tracker.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
@Slf4j
public class DataOpsController {

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String JSON_CONTENT_TYPE = "application/json";

    private final QuestionRepository questionRepository;
    private final ExcelExportService excelExportService;
    private final BackupService backupService;
    private final ObjectMapper objectMapper;

    @GetMapping("/export/excel")
    public ResponseEntity<InputStreamResource> exportToExcel() {
        log.info("Excel export requested");

        try {
            List<Question> questions = questionRepository.findAll();
            ByteArrayInputStream excelStream = excelExportService.exportQuestionsWithProgress(questions);
            String fileName = "dsa-tracker_" + Instant.now().toEpochMilli() + ".xlsx";
            log.info("Excel export prepared | questions={} | file={}", questions.size(), fileName);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName).contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE)).body(new InputStreamResource(excelStream));
        } catch (Exception ex) {
            log.error("Excel export failed", ex);
            throw new RuntimeException("Excel export failed", ex);
        }
    }

    @GetMapping("/backup")
    public ResponseEntity<String> triggerBackup() {
        log.info("Manual backup triggered via API");
        backupService.backupNow("MANUAL_API_TRIGGER");
        return ResponseEntity.ok("Backup triggered successfully");
    }

    @PostMapping(value = "/import/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importFromJson(@RequestParam("file") MultipartFile file) {
        log.info("JSON import requested | file={} | size={} bytes", file.getOriginalFilename(), file.getSize());
        try {
            return ResponseEntity.ok(backupService.restore(file));
        } catch (Exception e) {
            return ResponseEntity.ok(e.getMessage());
        }
    }
}
