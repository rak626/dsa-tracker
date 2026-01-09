package com.rakesh.dsa.tracker.controller.api;

import com.rakesh.dsa.tracker.model.Question;
import com.rakesh.dsa.tracker.repository.QuestionRepository;
import com.rakesh.dsa.tracker.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ExportController {

    private final QuestionRepository questionRepository;
    private final ExcelExportService excelExportService;

    @GetMapping("/export/excel")
    public ResponseEntity<InputStreamResource> exportToExcel() throws IOException {

        List<Question> questions = questionRepository.findAll();

        ByteArrayInputStream excelStream =
                excelExportService.exportQuestionsWithProgress(questions);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=dsa-tracker"  +
                "_" + Instant.now().toString() + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(new InputStreamResource(excelStream));
    }
}

