package com.academy.project.util;

import com.academy.project.dto.test.AddQuestionsRequest;
import com.academy.project.exception.ApiException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Sample Excel template + parse for bulk MCQ import.
 *
 * Columns: questionText | optionA | optionB | optionC | optionD | correctOption
 * correctOption must be A, B, C, or D (matching a filled option).
 */
public final class QuestionExcelHelper {

    public static final String SAMPLE_FILENAME = "test-questions-sample.xlsx";

    private static final String[] HEADERS = {
            "questionText", "optionA", "optionB", "optionC", "optionD", "correctOption"
    };

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "application/octet-stream"
    );

    private QuestionExcelHelper() {
    }

    public static byte[] buildSampleWorkbook() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet questions = workbook.createSheet("Questions");
            Sheet instructions = workbook.createSheet("Instructions");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = questions.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Example rows — replace or delete before upload
            writeRow(questions, 1,
                    "What is the capital of India?",
                    "Mumbai", "New Delhi", "Chennai", "Kolkata", "B");
            writeRow(questions, 2,
                    "Which planet is known as the Red Planet?",
                    "Earth", "Venus", "Mars", "Jupiter", "C");

            for (int i = 0; i < HEADERS.length; i++) {
                questions.autoSizeColumn(i);
            }

            String[] lines = {
                    "How to use this template",
                    "1. Fill each row with one MCQ on the Questions sheet.",
                    "2. optionA and optionB are required. optionC and optionD are optional.",
                    "3. correctOption must be A, B, C, or D (the letter of the correct option).",
                    "4. Delete the sample example rows before uploading (or keep them if you want those questions).",
                    "5. Save as .xlsx and upload via POST /api/admin/tests/{testId}/questions/excel",
                    "6. Exactly one correct option per question (the letter you set in correctOption)."
            };
            for (int i = 0; i < lines.length; i++) {
                Row row = instructions.createRow(i);
                row.createCell(0).setCellValue(lines[i]);
            }
            instructions.autoSizeColumn(0);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ApiException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate sample Excel: " + ex.getMessage()
            );
        }
    }

    public static AddQuestionsRequest parse(MultipartFile file) {
        validateFile(file);

        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheet("Questions");
            if (sheet == null) {
                sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            }
            if (sheet == null) {
                throw ApiException.badRequest("Excel file has no sheets");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw ApiException.badRequest("Excel file is missing a header row");
            }
            validateHeaders(headerRow);

            List<AddQuestionsRequest.QuestionItem> questions = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row)) {
                    continue;
                }

                String questionText = cellString(row.getCell(0));
                String optionA = cellString(row.getCell(1));
                String optionB = cellString(row.getCell(2));
                String optionC = cellString(row.getCell(3));
                String optionD = cellString(row.getCell(4));
                String correctRaw = cellString(row.getCell(5));

                int excelRow = r + 1; // 1-based for error messages
                if (questionText == null) {
                    throw ApiException.badRequest("Row " + excelRow + ": questionText is required");
                }
                if (optionA == null || optionB == null) {
                    throw ApiException.badRequest("Row " + excelRow + ": optionA and optionB are required");
                }
                if (correctRaw == null) {
                    throw ApiException.badRequest("Row " + excelRow + ": correctOption is required (A, B, C, or D)");
                }

                char correctLetter = normalizeCorrectOption(correctRaw, excelRow);
                List<String> optionTexts = new ArrayList<>();
                optionTexts.add(optionA);
                optionTexts.add(optionB);
                if (optionC != null) {
                    optionTexts.add(optionC);
                }
                if (optionD != null) {
                    optionTexts.add(optionD);
                }

                int correctIndex = correctLetter - 'A';
                if (correctIndex < 0 || correctIndex >= optionTexts.size()) {
                    throw ApiException.badRequest(
                            "Row " + excelRow + ": correctOption '" + correctLetter
                                    + "' does not match a filled option"
                    );
                }

                AddQuestionsRequest.QuestionItem item = new AddQuestionsRequest.QuestionItem();
                item.setQuestionText(questionText);
                item.setOrderIndex(null);

                List<AddQuestionsRequest.OptionItem> options = new ArrayList<>();
                for (int i = 0; i < optionTexts.size(); i++) {
                    AddQuestionsRequest.OptionItem opt = new AddQuestionsRequest.OptionItem();
                    opt.setOptionText(optionTexts.get(i));
                    opt.setCorrect(i == correctIndex);
                    opt.setOrderIndex(i + 1);
                    options.add(opt);
                }
                item.setOptions(options);
                questions.add(item);
            }

            if (questions.isEmpty()) {
                throw ApiException.badRequest("Excel has no question rows. Add at least one question below the header.");
            }

            AddQuestionsRequest request = new AddQuestionsRequest();
            request.setQuestions(questions);
            return request;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ApiException.badRequest("Failed to read Excel file: " + ex.getMessage());
        }
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Excel file is required");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw ApiException.badRequest("Only Excel files (.xlsx) are allowed");
        }
        String contentType = file.getContentType();
        if (contentType != null
                && !contentType.isBlank()
                && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            // Some browsers send odd types; filename check above is the main gate.
            if (!contentType.contains("sheet") && !contentType.contains("excel") && !contentType.contains("octet")) {
                throw ApiException.badRequest("Only Excel files (.xlsx) are allowed");
            }
        }
    }

    private static void validateHeaders(Row headerRow) {
        for (int i = 0; i < HEADERS.length; i++) {
            String actual = cellString(headerRow.getCell(i));
            if (actual == null || !HEADERS[i].equalsIgnoreCase(actual.trim())) {
                throw ApiException.badRequest(
                        "Invalid header in column " + (i + 1) + ". Expected '" + HEADERS[i]
                                + "'. Download the sample Excel and keep the header row."
                );
            }
        }
    }

    private static char normalizeCorrectOption(String raw, int excelRow) {
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (value.length() == 1 && value.charAt(0) >= 'A' && value.charAt(0) <= 'D') {
            return value.charAt(0);
        }
        // Allow 1-4 as aliases for A-D
        if (value.equals("1") || value.equals("2") || value.equals("3") || value.equals("4")) {
            return (char) ('A' + (value.charAt(0) - '1'));
        }
        throw ApiException.badRequest(
                "Row " + excelRow + ": correctOption must be A, B, C, or D (got '" + raw + "')"
        );
    }

    private static void writeRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private static boolean isBlankRow(Row row) {
        for (int i = 0; i < HEADERS.length; i++) {
            if (cellString(row.getCell(i)) != null) {
                return false;
            }
        }
        return true;
    }

    private static String cellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        String value = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    yield String.valueOf((long) num);
                }
                yield String.valueOf(num);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception ex) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            case BLANK -> null;
            default -> null;
        };
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }
}
