package com.academy.project.controller.admin;

import com.academy.project.dto.response.ApiResponse;
import com.academy.project.dto.response.PagedResponse;
import com.academy.project.dto.test.*;
import com.academy.project.enums.TestStatus;
import com.academy.project.service.test.TestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tests")
@RequiredArgsConstructor
@CrossOrigin
public class AdminTestController {

    private final TestService testService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestResponse>> createTest(
            @Valid @ModelAttribute CreateTestRequest request,
            @RequestParam(value = "pdf", required = false) MultipartFile pdf) {
        TestResponse response = testService.createTest(request, pdf);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Test created successfully", response));
    }

    @PutMapping(value = "/{testId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestResponse>> updateTest(
            @PathVariable String testId,
            @Valid @ModelAttribute UpdateTestRequest request,
            @RequestParam(value = "pdf", required = false) MultipartFile pdf) {
        TestResponse response = testService.updateTest(testId, request, pdf);
        return ResponseEntity.ok(ApiResponse.ok("Test updated successfully", response));
    }

    @PatchMapping("/{testId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestResponse>> publishTest(@PathVariable String testId) {
        TestResponse response = testService.publishTest(testId);
        return ResponseEntity.ok(ApiResponse.ok("Test published successfully", response));
    }

    @PatchMapping("/{testId}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestResponse>> unpublishTest(@PathVariable String testId) {
        TestResponse response = testService.unpublishTest(testId);
        return ResponseEntity.ok(ApiResponse.ok("Test unpublished successfully", response));
    }

    @PostMapping("/{testId}/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestResponse>> addQuestions(
            @PathVariable String testId,
            @Valid @RequestBody AddQuestionsRequest request) {
        TestResponse response = testService.addQuestions(testId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Questions added successfully", response));
    }

    @DeleteMapping("/{testId}/questions/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable String testId,
            @PathVariable Long questionId) {
        testService.deleteQuestion(testId, questionId);
        return ResponseEntity.ok(ApiResponse.ok("Question deleted successfully", null));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<TestResponse>>> listTests(
            @RequestParam(required = false) TestStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<TestResponse> response = testService.listTests(status, search, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Tests fetched successfully", response));
    }

    @GetMapping("/{testId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestResponse>> getTest(@PathVariable String testId) {
        TestResponse response = testService.getTestForAdmin(testId);
        return ResponseEntity.ok(ApiResponse.ok("Test fetched successfully", response));
    }

    @GetMapping("/{testId}/attempts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TestResultResponse>>> listAttempts(@PathVariable String testId) {
        List<TestResultResponse> response = testService.listAttempts(testId);
        return ResponseEntity.ok(ApiResponse.ok("Attempts fetched successfully", response));
    }

    @DeleteMapping("/{testId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTest(@PathVariable String testId) {
        testService.deleteTest(testId);
        return ResponseEntity.ok(ApiResponse.ok("Test deleted successfully", null));
    }
}
