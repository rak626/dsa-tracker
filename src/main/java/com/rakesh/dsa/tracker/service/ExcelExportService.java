package com.rakesh.dsa.tracker.service;

import com.rakesh.dsa.tracker.model.Question;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    public ByteArrayInputStream exportQuestionsWithProgress(
            List<Question> questions) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("DSA Tracker");

        CreationHelper creationHelper = workbook.getCreationHelper();

        // ---------- Styles ----------
        CellStyle bodyStyle = createBodyStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);

        CellStyle solvedStyle = createColoredBodyStyle(
                workbook, IndexedColors.LIGHT_GREEN);
        CellStyle unsolvedStyle = createColoredBodyStyle(
                workbook, IndexedColors.ROSE);

        CellStyle easyStyle = createColoredBodyStyle(
                workbook, IndexedColors.LIGHT_GREEN);
        CellStyle mediumStyle = createColoredBodyStyle(
                workbook, IndexedColors.LIGHT_YELLOW);
        CellStyle hardStyle = createColoredBodyStyle(
                workbook, IndexedColors.CORAL);

        CellStyle linkStyle = createLinkStyle(workbook);

        // ---------- Header ----------
        String[] headers = {
                "Video ID",
                "Problem Name",
                "Platform",
                "Difficulty",
                "Solved",
                "Revise Count",
                "Problem Link",
                "Topics",
                "Patterns"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // ---------- Data ----------
        int rowIdx = 1;
        for (Question q : questions) {

            Row row = sheet.createRow(rowIdx++);

            createCell(row, 0, q.getVideoId(), bodyStyle);
            createCell(row, 1, q.getProblemName(), bodyStyle);
            createCell(row, 2, q.getPlatform(), bodyStyle);

            // Difficulty (colored)
            CellStyle diffStyle = switch (q.getDifficulty().toLowerCase()) {
                case "easy" -> easyStyle;
                case "medium" -> mediumStyle;
                case "hard" -> hardStyle;
                default -> bodyStyle;
            };
            createCell(row, 3, q.getDifficulty(), diffStyle);

            // Solved (colored)
            createCell(
                    row,
                    4,
                    q.isSolved() ? "Yes" : "No",
                    q.isSolved() ? solvedStyle : unsolvedStyle
            );

            // Revise Count (NEW COLUMN)
            createCell(
                    row,
                    5,
                    String.valueOf(q.getReviseCount()),
                    bodyStyle
            );

            // Problem Link (clickable hyperlink)
            Cell linkCell = row.createCell(6);
            linkCell.setCellValue(q.getProblemLink());
            linkCell.setCellStyle(linkStyle);

            Hyperlink hyperlink =
                    creationHelper.createHyperlink(HyperlinkType.URL);
            hyperlink.setAddress(q.getProblemLink());
            linkCell.setHyperlink(hyperlink);

            createCell(row, 7, String.join(", ", q.getTopics()), bodyStyle);
            createCell(row, 8, String.join(", ", q.getPatterns()), bodyStyle);
        }

        // ---------- Sheet Enhancements ----------
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        sheet.createFreezePane(0, 1);

        // ---------- Output ----------
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return new ByteArrayInputStream(out.toByteArray());
    }

    // =========================================================
    // Helper Methods
    // =========================================================

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = createBaseStyle(wb);

        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    private CellStyle createBodyStyle(Workbook wb) {
        return createBaseStyle(wb);
    }

    private CellStyle createColoredBodyStyle(Workbook wb, IndexedColors color) {
        CellStyle style = createBaseStyle(wb);

        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    private CellStyle createLinkStyle(Workbook wb) {
        CellStyle style = createBaseStyle(wb);

        Font font = wb.createFont();
        font.setUnderline(Font.U_SINGLE);
        font.setColor(IndexedColors.BLUE.getIndex());
        style.setFont(font);

        return style;
    }

    private CellStyle createBaseStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();

        // Borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());

        // Alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Wrap text
        style.setWrapText(true);

        return style;
    }
}
