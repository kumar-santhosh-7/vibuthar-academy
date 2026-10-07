package com.academy.project.serviceImplementation.test;

import com.academy.project.dto.response.PagedResponse;
import com.academy.project.dto.test.*;
import com.academy.project.entity.test.*;
import com.academy.project.enums.AttemptStatus;
import com.academy.project.enums.TestStatus;
import com.academy.project.exception.ApiException;
import com.academy.project.repository.test.*;
import com.academy.project.repository.user.UserRepository;
import com.academy.project.security.SecurityUtils;
import com.academy.project.service.test.TestService;
import com.academy.project.util.QuestionExcelHelper;
import com.academy.project.util.TestIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestServiceImplementation implements TestService {

    private static final Set<String> ALLOWED_PDF_TYPES = Set.of("application/pdf");

    private final OnlineTestRepository onlineTestRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final UserRepository userRepository;

    @Value("${app.pdfs.storage-dir:pdfs}")
    private String pdfStorageDir;

    @Value("${app.pdfs.url-prefix:/pdfs}")
    private String pdfUrlPrefix;

    @Override
    @Transactional
    public TestResponse createTest(CreateTestRequest request, MultipartFile pdf) {
        String pdfUrl = storePdfIfPresent(pdf);

        OnlineTest test = OnlineTest.builder()
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .durationMinutes(request.getDurationMinutes())
                .totalMarks(request.getTotalMarks())
                .cutOff(request.getCutOff())
                .pdfUrl(pdfUrl)
                .status(TestStatus.DRAFT)
                .build();

        test = onlineTestRepository.save(test);
        test.setTestId(TestIdGenerator.generate(test));
        test = onlineTestRepository.save(test);

        return TestResponse.summary(test, 0);
    }

    @Override
    @Transactional
    public TestResponse updateTest(String testId, UpdateTestRequest request, MultipartFile pdf) {
        OnlineTest test = requireTest(testId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            test.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            test.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getDurationMinutes() != null) {
            test.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getTotalMarks() != null) {
            test.setTotalMarks(request.getTotalMarks());
        }
        if (request.getCutOff() != null) {
            test.setCutOff(request.getCutOff());
        }
        if (pdf != null && !pdf.isEmpty()) {
            String oldPdfUrl = test.getPdfUrl();
            test.setPdfUrl(storePdfIfPresent(pdf));
            deletePdfQuietly(oldPdfUrl);
        }

        test = onlineTestRepository.save(test);
        int count = (int) testQuestionRepository.countByTestPk(test.getId());
        return TestResponse.summary(test, count);
    }

    @Override
    @Transactional
    public TestResponse publishTest(String testId) {
        OnlineTest test = requireTest(testId);
        long questionCount = testQuestionRepository.countByTestPk(test.getId());
        if (questionCount == 0) {
            throw ApiException.badRequest("Add at least one question before publishing");
        }
        test.setStatus(TestStatus.PUBLISHED);
        test = onlineTestRepository.save(test);
        return TestResponse.summary(test, (int) questionCount);
    }

    @Override
    @Transactional
    public TestResponse unpublishTest(String testId) {
        OnlineTest test = requireTest(testId);
        test.setStatus(TestStatus.DRAFT);
        test = onlineTestRepository.save(test);
        int count = (int) testQuestionRepository.countByTestPk(test.getId());
        return TestResponse.summary(test, count);
    }

    @Override
    @Transactional
    public void deleteTest(String testId) {
        OnlineTest test = requireTest(testId);
        Long testPk = test.getId();

        List<TestQuestion> questions = testQuestionRepository.findByTestPkOrderByOrderIndexAscIdAsc(testPk);
        List<Long> questionIds = questions.stream().map(TestQuestion::getId).toList();
        if (!questionIds.isEmpty()) {
            questionOptionRepository.deleteByQuestionIdIn(questionIds);
        }

        List<TestAttempt> attempts = testAttemptRepository.findByTestPkOrderBySubmittedAtDesc(testPk);
        if (!attempts.isEmpty()) {
            attemptAnswerRepository.deleteByAttemptIdIn(
                    attempts.stream().map(TestAttempt::getId).toList()
            );
            testAttemptRepository.deleteByTestPk(testPk);
        }

        testQuestionRepository.deleteByTestPk(testPk);
        onlineTestRepository.delete(test);
        deletePdfQuietly(test.getPdfUrl());
    }

    @Override
    @Transactional
    public TestResponse addQuestions(String testId, AddQuestionsRequest request) {
        OnlineTest test = requireTest(testId);
        int existingCount = (int) testQuestionRepository.countByTestPk(test.getId());

        int index = 0;
        for (AddQuestionsRequest.QuestionItem item : request.getQuestions()) {
            validateQuestionItem(item);

            int orderIndex = item.getOrderIndex() != null ? item.getOrderIndex() : existingCount + index + 1;

            TestQuestion question = TestQuestion.builder()
                    .testPk(test.getId())
                    .questionText(item.getQuestionText().trim())
                    .orderIndex(orderIndex)
                    .build();
            question = testQuestionRepository.save(question);

            int optIndex = 0;
            for (AddQuestionsRequest.OptionItem optionItem : item.getOptions()) {
                int optionOrder = optionItem.getOrderIndex() != null
                        ? optionItem.getOrderIndex()
                        : optIndex + 1;

                QuestionOption option = QuestionOption.builder()
                        .questionId(question.getId())
                        .optionText(optionItem.getOptionText().trim())
                        .correct(Boolean.TRUE.equals(optionItem.getCorrect()))
                        .orderIndex(optionOrder)
                        .build();
                questionOptionRepository.save(option);
                optIndex++;
            }
            index++;
        }

        return buildDetailResponse(test, true);
    }

    @Override
    public byte[] downloadSampleQuestionsExcel() {
        return QuestionExcelHelper.buildSampleWorkbook();
    }

    @Override
    @Transactional
    public TestResponse importQuestionsFromExcel(String testId, MultipartFile excelFile) {
        AddQuestionsRequest request = QuestionExcelHelper.parse(excelFile);
        return addQuestions(testId, request);
    }

    @Override
    @Transactional
    public void deleteQuestion(String testId, Long questionId) {
        OnlineTest test = requireTest(testId);
        TestQuestion question = testQuestionRepository.findById(questionId)
                .orElseThrow(() -> ApiException.notFound("Question not found"));

        if (!Objects.equals(question.getTestPk(), test.getId())) {
            throw ApiException.badRequest("Question does not belong to this test");
        }

        questionOptionRepository.deleteByQuestionId(questionId);
        testQuestionRepository.delete(question);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TestResponse> listTests(TestStatus status, String search, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<OnlineTest> testPage;
        if (search != null && !search.isBlank()) {
            if (status != null) {
                testPage = onlineTestRepository.findByStatusAndTitleContainingIgnoreCase(
                        status, search.trim(), pageRequest);
            } else {
                testPage = onlineTestRepository.findByTitleContainingIgnoreCase(search.trim(), pageRequest);
            }
        } else if (status != null) {
            testPage = onlineTestRepository.findByStatus(status, pageRequest);
        } else {
            testPage = onlineTestRepository.findAll(pageRequest);
        }

        Page<TestResponse> mapped = testPage.map(t ->
                TestResponse.summary(t, (int) testQuestionRepository.countByTestPk(t.getId()))
        );
        return PagedResponse.from(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public TestResponse getTestForAdmin(String testId) {
        OnlineTest test = requireTest(testId);
        return buildDetailResponse(test, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TestResponse> listPublishedTests(String search, int page, int size) {
        return listTests(TestStatus.PUBLISHED, search, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public TestResponse getTestForStudent(String testId) {
        OnlineTest test = requirePublishedTest(testId);
        return buildDetailResponse(test, false);
    }

    @Override
    @Transactional
    public TestResultResponse startTest(String testId) {
        String userId = requireAuthenticatedUserId();
        requireActiveUser(userId);

        OnlineTest test = requirePublishedTest(testId);

        Optional<TestAttempt> existing = testAttemptRepository.findByTestPkAndUserId(test.getId(), userId);
        if (existing.isPresent()) {
            TestAttempt attempt = existing.get();
            if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
                throw ApiException.conflict("You have already submitted this test");
            }
            return TestResultResponse.from(attempt, test);
        }

        long questionCount = testQuestionRepository.countByTestPk(test.getId());
        if (questionCount == 0) {
            throw ApiException.badRequest("This test has no questions");
        }

        TestAttempt attempt = TestAttempt.builder()
                .testPk(test.getId())
                .userId(userId)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();
        attempt = testAttemptRepository.save(attempt);
        return TestResultResponse.from(attempt, test);
    }

    @Override
    @Transactional
    public TestResultResponse submitTest(String testId, SubmitTestRequest request) {
        String userId = requireAuthenticatedUserId();
        requireActiveUser(userId);

        OnlineTest test = requirePublishedTest(testId);

        TestAttempt attempt = testAttemptRepository.findByTestPkAndUserId(test.getId(), userId)
                .orElse(null);

        if (attempt != null && attempt.getStatus() == AttemptStatus.SUBMITTED) {
            throw ApiException.conflict("You have already submitted this test");
        }

        LocalDateTime startedAt = attempt != null && attempt.getStartedAt() != null
                ? attempt.getStartedAt()
                : LocalDateTime.now();

        List<TestQuestion> questions = testQuestionRepository.findByTestPkOrderByOrderIndexAscIdAsc(test.getId());
        if (questions.isEmpty()) {
            throw ApiException.badRequest("This test has no questions");
        }

        Map<Long, TestQuestion> questionById = questions.stream()
                .collect(Collectors.toMap(TestQuestion::getId, q -> q));

        List<Long> questionIds = questions.stream().map(TestQuestion::getId).toList();
        Map<Long, List<QuestionOption>> optionsByQuestion = questionOptionRepository
                .findByQuestionIdInOrderByOrderIndexAscIdAsc(questionIds)
                .stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));

        Map<Long, Long> selectedByQuestion = new LinkedHashMap<>();
        for (SubmitTestRequest.AnswerItem answer : request.getAnswers()) {
            if (selectedByQuestion.containsKey(answer.getQuestionId())) {
                throw ApiException.badRequest("Duplicate answer for question id " + answer.getQuestionId());
            }
            selectedByQuestion.put(answer.getQuestionId(), answer.getOptionId());
        }

        if (selectedByQuestion.size() != questions.size()) {
            throw ApiException.badRequest(
                    "Please answer all questions. Expected " + questions.size()
                            + ", received " + selectedByQuestion.size()
            );
        }

        int correctAnswers = 0;
        List<AttemptAnswer> answerEntities = new ArrayList<>();

        for (TestQuestion question : questions) {
            Long selectedOptionId = selectedByQuestion.get(question.getId());
            if (selectedOptionId == null) {
                throw ApiException.badRequest("Missing answer for question id " + question.getId());
            }
            if (!questionById.containsKey(question.getId())) {
                throw ApiException.badRequest("Invalid question id " + question.getId());
            }

            List<QuestionOption> options = optionsByQuestion.getOrDefault(question.getId(), List.of());
            QuestionOption selected = options.stream()
                    .filter(o -> Objects.equals(o.getId(), selectedOptionId))
                    .findFirst()
                    .orElseThrow(() -> ApiException.badRequest(
                            "Option " + selectedOptionId + " does not belong to question " + question.getId()
                    ));

            boolean correct = selected.isCorrect();
            if (correct) {
                correctAnswers++;
            }

            answerEntities.add(AttemptAnswer.builder()
                    .questionId(question.getId())
                    .selectedOptionId(selected.getId())
                    .correct(correct)
                    .build());
        }

        int total = questions.size();
        int incorrectAnswers = total - correctAnswers;
        int totalMarks = test.getTotalMarks() != null ? test.getTotalMarks() : total;
        int marksObtained = total == 0 ? 0 : (correctAnswers * totalMarks) / total;
        double percentage = totalMarks == 0 ? 0.0 : Math.round((marksObtained * 10000.0) / totalMarks) / 100.0;

        LocalDateTime submittedAt = LocalDateTime.now();
        long timeTakenSeconds = Math.max(0, Duration.between(startedAt, submittedAt).getSeconds());

        if (attempt == null) {
            attempt = TestAttempt.builder()
                    .testPk(test.getId())
                    .userId(userId)
                    .startedAt(startedAt)
                    .build();
        }

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setScore(correctAnswers);
        attempt.setCorrectAnswers(correctAnswers);
        attempt.setIncorrectAnswers(incorrectAnswers);
        attempt.setMarksObtained(marksObtained);
        attempt.setTotalQuestions(total);
        attempt.setPercentage(percentage);
        attempt.setTimeTakenSeconds((int) Math.min(timeTakenSeconds, Integer.MAX_VALUE));
        attempt.setStartedAt(startedAt);
        attempt.setSubmittedAt(submittedAt);
        attempt = testAttemptRepository.save(attempt);

        for (AttemptAnswer answer : answerEntities) {
            answer.setAttemptId(attempt.getId());
        }
        attemptAnswerRepository.saveAll(answerEntities);

        return TestResultResponse.from(attempt, test);
    }

    @Override
    @Transactional(readOnly = true)
    public TestResultResponse getMyAttempt(String testId) {
        String userId = requireAuthenticatedUserId();
        OnlineTest test = requireTest(testId);

        TestAttempt attempt = testAttemptRepository.findByTestPkAndUserId(test.getId(), userId)
                .orElseThrow(() -> ApiException.notFound("No attempt found for this test"));

        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw ApiException.badRequest("Test has not been submitted yet");
        }

        return TestResultResponse.from(attempt, test);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultResponse> listAttempts(String testId) {
        OnlineTest test = requireTest(testId);
        return testAttemptRepository
                .findByTestPkAndStatusOrderBySubmittedAtDesc(test.getId(), AttemptStatus.SUBMITTED)
                .stream()
                .map(a -> TestResultResponse.from(a, test))
                .toList();
    }

    private void requireActiveUser(String userId) {
        userRepository.findByUserId(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private TestResponse buildDetailResponse(OnlineTest test, boolean includeCorrect) {
        List<TestQuestion> questions = testQuestionRepository.findByTestPkOrderByOrderIndexAscIdAsc(test.getId());
        if (questions.isEmpty()) {
            return TestResponse.withQuestions(test, List.of());
        }

        List<Long> questionIds = questions.stream().map(TestQuestion::getId).toList();
        Map<Long, List<QuestionOption>> optionsByQuestion = questionOptionRepository
                .findByQuestionIdInOrderByOrderIndexAscIdAsc(questionIds)
                .stream()
                .collect(Collectors.groupingBy(
                        QuestionOption::getQuestionId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<QuestionResponse> questionResponses = questions.stream()
                .map(q -> QuestionResponse.from(
                        q,
                        optionsByQuestion.getOrDefault(q.getId(), List.of()),
                        includeCorrect
                ))
                .toList();

        return TestResponse.withQuestions(test, questionResponses);
    }

    private void validateQuestionItem(AddQuestionsRequest.QuestionItem item) {
        if (item.getOptions() == null || item.getOptions().size() < 2) {
            throw ApiException.badRequest("Each question must have at least 2 options");
        }
        long correctCount = item.getOptions().stream()
                .filter(o -> Boolean.TRUE.equals(o.getCorrect()))
                .count();
        if (correctCount != 1) {
            throw ApiException.badRequest("Each question must have exactly one correct option");
        }
    }

    private OnlineTest requireTest(String testId) {
        return onlineTestRepository.findByTestId(testId)
                .orElseThrow(() -> ApiException.notFound("Test not found"));
    }

    private OnlineTest requirePublishedTest(String testId) {
        OnlineTest test = requireTest(testId);
        if (test.getStatus() != TestStatus.PUBLISHED) {
            throw ApiException.notFound("Test not found");
        }
        return test;
    }

    private String requireAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = SecurityUtils.resolveUserId(auth);
        if (userId == null) {
            throw ApiException.unauthorized("Login required");
        }
        return userId;
    }

    private String storePdfIfPresent(MultipartFile pdf) {
        if (pdf == null || pdf.isEmpty()) {
            return null;
        }

        String contentType = pdf.getContentType();
        if (contentType == null || !ALLOWED_PDF_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw ApiException.badRequest("Only PDF files are allowed");
        }

        String originalFileName = StringUtils.cleanPath(
                pdf.getOriginalFilename() != null ? pdf.getOriginalFilename() : "reference.pdf"
        );
        if (!originalFileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw ApiException.badRequest("Only PDF files are allowed");
        }

        String storedFileName = UUID.randomUUID().toString().replace("-", "") + ".pdf";
        Path uploadPath = Paths.get(pdfStorageDir).toAbsolutePath().normalize();
        Path target = uploadPath.resolve(storedFileName).normalize();
        if (!target.startsWith(uploadPath)) {
            throw ApiException.badRequest("Invalid file path");
        }

        try {
            Files.createDirectories(uploadPath);
            Files.copy(pdf.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored test PDF at {}", target);
        } catch (IOException ex) {
            log.error("Failed to store PDF. path={}, user.dir={}, cause={}",
                    target, System.getProperty("user.dir"), ex.toString(), ex);
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store PDF file (" + uploadPath + "): " + ex.getMessage()
            );
        }

        String prefix = pdfUrlPrefix.endsWith("/")
                ? pdfUrlPrefix.substring(0, pdfUrlPrefix.length() - 1)
                : pdfUrlPrefix;
        return prefix + "/" + storedFileName;
    }

    private void deletePdfQuietly(String pdfUrl) {
        if (pdfUrl == null || pdfUrl.isBlank()) {
            return;
        }

        String prefix = pdfUrlPrefix.endsWith("/")
                ? pdfUrlPrefix.substring(0, pdfUrlPrefix.length() - 1)
                : pdfUrlPrefix;
        String expectedPrefix = prefix + "/";
        if (!pdfUrl.startsWith(expectedPrefix)) {
            return;
        }

        String fileName = pdfUrl.substring(expectedPrefix.length());
        Path target = Paths.get(pdfStorageDir).toAbsolutePath().normalize().resolve(fileName).normalize();
        Path uploadPath = Paths.get(pdfStorageDir).toAbsolutePath().normalize();
        if (!target.startsWith(uploadPath)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("Could not delete PDF {}: {}", target, ex.toString());
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
