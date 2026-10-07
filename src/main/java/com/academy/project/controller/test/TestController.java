package com.academy.project.controller.test;

import com.academy.project.dto.response.ApiResponse;
import com.academy.project.dto.response.PagedResponse;
import com.academy.project.dto.test.SubmitTestRequest;
import com.academy.project.dto.test.TestResponse;
import com.academy.project.dto.test.TestResultResponse;
import com.academy.project.service.test.TestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@CrossOrigin
public class TestController {

    private final TestService testService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<TestResponse>>> listPublishedTests(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<TestResponse> response = testService.listPublishedTests(search, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Tests fetched successfully", response));
    }

    @GetMapping("/{testId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TestResponse>> getTest(@PathVariable String testId) {
        TestResponse response = testService.getTestForStudent(testId);
        return ResponseEntity.ok(ApiResponse.ok("Test fetched successfully", response));
    }

    @PostMapping("/{testId}/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TestResultResponse>> startTest(@PathVariable String testId) {
        TestResultResponse response = testService.startTest(testId);
        return ResponseEntity.ok(ApiResponse.ok("Test started successfully", response));
    }

    @PostMapping("/{testId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TestResultResponse>> submitTest(
            @PathVariable String testId,
            @Valid @RequestBody SubmitTestRequest request) {
        TestResultResponse response = testService.submitTest(testId, request);
        return ResponseEntity.ok(ApiResponse.ok("Test submitted successfully", response));
    }

    @GetMapping("/{testId}/my-attempt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TestResultResponse>> getMyAttempt(@PathVariable String testId) {
        TestResultResponse response = testService.getMyAttempt(testId);
        return ResponseEntity.ok(ApiResponse.ok("Attempt fetched successfully", response));
    }
}
