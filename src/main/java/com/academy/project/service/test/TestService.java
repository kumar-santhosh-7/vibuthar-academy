package com.academy.project.service.test;

import com.academy.project.dto.response.PagedResponse;
import com.academy.project.dto.test.*;
import com.academy.project.enums.TestStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TestService {

    TestResponse createTest(CreateTestRequest request, MultipartFile pdf);

    TestResponse updateTest(String testId, UpdateTestRequest request, MultipartFile pdf);

    TestResponse publishTest(String testId);

    TestResponse unpublishTest(String testId);

    void deleteTest(String testId);

    TestResponse addQuestions(String testId, AddQuestionsRequest request);

    /** Empty/sample Excel template for bulk question upload. */
    byte[] downloadSampleQuestionsExcel();

    /** Import questions from filled sample Excel. */
    TestResponse importQuestionsFromExcel(String testId, MultipartFile excelFile);

    void deleteQuestion(String testId, Long questionId);

    PagedResponse<TestResponse> listTests(TestStatus status, String search, int page, int size);

    TestResponse getTestForAdmin(String testId);

    PagedResponse<TestResponse> listPublishedTests(String search, int page, int size);

    TestResponse getTestForStudent(String testId);

    /** Starts the timer for the current student. Call before taking the test. */
    TestResultResponse startTest(String testId);

    TestResultResponse submitTest(String testId, SubmitTestRequest request);

    TestResultResponse getMyAttempt(String testId);

    List<TestResultResponse> listAttempts(String testId);
}
