package com.quizmind.adaptive_quiz;

import com.quizmind.adaptive_quiz.controller.PdfController;
import com.quizmind.adaptive_quiz.controller.QuestionController;
import com.quizmind.adaptive_quiz.controller.TeacherController;
import com.quizmind.adaptive_quiz.controller.UserController;
import com.quizmind.adaptive_quiz.model.*;
import com.quizmind.adaptive_quiz.model.dto.*;
import com.quizmind.adaptive_quiz.repository.PdfDocumentRepository;
import com.quizmind.adaptive_quiz.repository.QuestionRepository;
import com.quizmind.adaptive_quiz.repository.QuizAttemptRepository;
import com.quizmind.adaptive_quiz.repository.StudentTopicProgressRepository;
import com.quizmind.adaptive_quiz.repository.UserRepository;
import com.quizmind.adaptive_quiz.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AdaptiveQuizApplicationTests {

    @Autowired
    private QuestionGenerationService questionGenerationService;

    @Autowired
    private AdaptiveQuizService adaptiveQuizService;

    @Autowired
    private WordSearchService wordSearchService;

    @Autowired
    private PerformanceAnalyticsService analyticsService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private PdfDocumentRepository pdfDocumentRepository;

    @Autowired
    private StudentTopicProgressRepository progressRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionController questionController;

    @Autowired
    private TeacherController teacherController;

    @Autowired
    private UserController userController;

    @Autowired
    private PdfController pdfController;

    @Test
    void testQuestionGenerationDeterministic() {
        List<String> sentences = Arrays.asList(
                "An operating system is system software that manages computer hardware and software resources.",
                "Encapsulation is defined as the technique of bundling data with methods that operate on that data.",
                "Virtual memory refers to a memory management technique that provides an idealized abstraction of storage."
        );

        PdfDocument testDoc = new PdfDocument("test_notes.pdf", 1024L, 3, "Sample test preview", "PROCESSED");
        testDoc = pdfDocumentRepository.save(testDoc);
        int count = questionGenerationService.generateQuestionsFromSentences(sentences, testDoc);

        assertTrue(count > 0, "Should generate deterministic questions from valid sentences");

        List<GeneratedQuestion> all = questionRepository.findAll();
        assertFalse(all.isEmpty(), "Questions should be persisted in database");
    }

    @Test
    void testQuestionByQuestionWordSearchWorkflow() {
        WordSearchInitResponse init = wordSearchService.createWordSearchSession(null, "TestStudent");
        assertNotNull(init);
        assertNotNull(init.getSessionToken());
        assertTrue(init.getSize() >= 10);
        assertNotNull(init.getGrid());
        assertTrue(init.getTotalQuestions() > 0);
        assertEquals(0, init.getCurrentQuestionIndex());

        // CRITICAL CHECK: Correct answer must NEVER be leaked in the client question payload
        assertNotNull(init.getCurrentQuestion());
        assertNull(init.getCurrentQuestion().getCorrectAnswer(), "Correct answer must NEVER be sent to student in Word Search!");

        // Test Incorrect Selection
        WordSearchValidationRequest wrongReq = new WordSearchValidationRequest();
        wrongReq.setSessionToken(init.getSessionToken());
        wrongReq.setSelectedWord("RANDOMWRONGWORD");

        WordSearchValidationResponse wrongRes = wordSearchService.validateWordSelection(wrongReq);
        assertFalse(wrongRes.isCorrect(), "Wrong selection must be marked incorrect");
        assertEquals("Wrong Answer! Try Again.", wrongRes.getMessage());
        assertFalse(wrongRes.isQuestionCompleted());
        assertNull(wrongRes.getFoundWord());

        var progress = wordSearchService.getSessionProgress(init.getSessionToken());
        assertEquals(0, progress.get("correctCount"));
        assertTrue((int) progress.get("incorrectAttempts") >= 1);
    }

    @Test
    void testAdaptiveDifficultyTransitions() {
        String testStudent = "AdaptiveRuleTestUser";
        String topic = "Adaptive Topic " + System.currentTimeMillis();

        for (int i = 1; i <= 4; i++) {
            GeneratedQuestion qEasy = new GeneratedQuestion(
                    "Easy Question " + i + " on " + topic, "AnsEasy" + i,
                    List.of("AnsEasy" + i, "OptB", "OptC", "OptD"), topic,
                    Difficulty.EASY, QuestionType.MCQ, "Source easy", "Exp easy", ApprovalStatus.APPROVED
            );
            GeneratedQuestion qMed = new GeneratedQuestion(
                    "Medium Question " + i + " on " + topic, "AnsMed" + i,
                    List.of("AnsMed" + i, "OptB", "OptC", "OptD"), topic,
                    Difficulty.MEDIUM, QuestionType.MCQ, "Source med", "Exp med", ApprovalStatus.APPROVED
            );
            questionRepository.save(qEasy);
            questionRepository.save(qMed);
        }

        var session = adaptiveQuizService.startQuizSession(testStudent, topic);
        String sessionId = (String) session.get("sessionId");

        StudentTopicProgress prog = progressRepository.findByStudentNameAndTopic(testStudent, topic).orElse(null);
        assertNotNull(prog);
        assertEquals(Difficulty.EASY, prog.getCurrentDifficulty());

        // Answer 3 correct in a row -> level up to MEDIUM
        for (int i = 1; i <= 3; i++) {
            QuestionDto nextQ = adaptiveQuizService.getNextAdaptiveQuestion(testStudent, topic, sessionId);
            assertNotNull(nextQ);

            GeneratedQuestion dbQ = questionRepository.findById(nextQ.getId()).orElseThrow();
            AnswerSubmissionDto sub = new AnswerSubmissionDto();
            sub.setQuestionId(dbQ.getId());
            sub.setSelectedAnswer(dbQ.getCorrectAnswer());
            sub.setResponseTimeSec(5);
            sub.setConfidence(Confidence.HIGH);
            sub.setSessionId(sessionId);
            sub.setStudentName(testStudent);

            QuizFeedbackDto fb = adaptiveQuizService.submitAnswer(sub);
            assertTrue(fb.isCorrect());
        }

        StudentTopicProgress progAfter3 = progressRepository.findByStudentNameAndTopic(testStudent, topic).orElse(null);
        assertNotNull(progAfter3);
        assertEquals(Difficulty.MEDIUM, progAfter3.getCurrentDifficulty(), "Should increase to MEDIUM difficulty after 3 consecutive correct answers");
    }

    @Test
    void testAnalyticsDashboard() {
        DashboardStatsDto stats = analyticsService.getDashboardStatistics("Student");
        assertNotNull(stats);
        assertTrue(stats.getTotalQuestionsAttempted() > 0, "Should have initial seeded attempts");
        assertNotNull(stats.getTopicPerformances());
        assertFalse(stats.getRecentActivity().isEmpty());
    }

    @Test
    void testMistakeAnalyzer() {
        List<QuizAttempt> mistakes = analyticsService.getMistakes("Student");
        assertNotNull(mistakes);
        assertFalse(mistakes.isEmpty(), "Initial seed includes mistakes for diagnostics");
        for (QuizAttempt m : mistakes) {
            assertFalse(m.isCorrect(), "Mistake records must be incorrect attempts");
        }
    }

    // ==========================================
    // 20. USER MANAGEMENT & AUTHENTICATION TESTS
    // ==========================================

    @Test
    void testUserRegistrationAndLoginLifecycle() {
        String uniqueSuffix = "" + System.currentTimeMillis();
        String username1 = "student_" + uniqueSuffix;
        String username2 = "other_" + uniqueSuffix;

        // 1. Register new username
        RegisterRequest reg1 = new RegisterRequest(username1, "pass1234", "Student One", "s1@test.com", UserRole.STUDENT);
        AuthResponse res1 = authService.register(reg1, null);
        assertTrue(res1.isSuccess());
        assertNotNull(res1.getToken());
        assertEquals(username1, res1.getUser().getUsername());

        // 2. Register another user with a different username
        RegisterRequest reg2 = new RegisterRequest(username2, "pass5678", "Student Two", "s2@test.com", UserRole.STUDENT);
        AuthResponse res2 = authService.register(reg2, null);
        assertTrue(res2.isSuccess());

        // 3. Attempt registration using an existing username
        RegisterRequest regDup = new RegisterRequest(username1, "anyPass", "Duplicate", "dup@test.com", UserRole.STUDENT);
        AuthResponse resDup = authService.register(regDup, null);
        assertFalse(resDup.isSuccess(), "Duplicate registration must be rejected");
        assertTrue(resDup.getMessage().contains("already taken"));

        // 4. Attempt registration using the same username with different letter casing
        RegisterRequest regCase = new RegisterRequest(username1.toUpperCase(), "anyPass", "Case Test", "case@test.com", UserRole.STUDENT);
        AuthResponse resCase = authService.register(regCase, null);
        assertFalse(resCase.isSuccess(), "Case-insensitive duplicate registration must be rejected");
        assertTrue(resCase.getMessage().contains("already taken"));

        // 5. Log in using an existing username and correct password
        AuthRequest loginValid = new AuthRequest(username1, "pass1234");
        AuthResponse loginResValid = authService.login(loginValid, null);
        assertTrue(loginResValid.isSuccess());
        assertNotNull(loginResValid.getToken());
        assertEquals("Student One", loginResValid.getUser().getFullName());

        // 6. Attempt login using an incorrect password
        AuthRequest loginInvalid = new AuthRequest(username1, "wrongPassword");
        AuthResponse loginResInvalid = authService.login(loginInvalid, null);
        assertFalse(loginResInvalid.isSuccess());
        assertEquals("Invalid username or password.", loginResInvalid.getMessage());

        // 7. Profile Update and Password Change
        User user1 = userRepository.findByUsernameIgnoreCase(username1).orElseThrow();
        UpdateProfileRequest upReq = new UpdateProfileRequest("Student One Updated", "updated@test.com", "My bio", "Tech University");
        UserProfileDto updatedProfile = userService.updateProfile(user1, upReq);
        assertEquals("Student One Updated", updatedProfile.getFullName());
        assertEquals("Tech University", updatedProfile.getInstitution());

        // Change password
        ChangePasswordRequest cpReq = new ChangePasswordRequest("pass1234", "newSecurePass123");
        var cpRes = userService.changePassword(user1, cpReq);
        assertTrue((Boolean) cpRes.get("success"));

        // Verify old password no longer works
        AuthResponse oldLogin = authService.login(new AuthRequest(username1, "pass1234"), null);
        assertFalse(oldLogin.isSuccess());

        // Verify new password works
        AuthResponse newLogin = authService.login(new AuthRequest(username1, "newSecurePass123"), null);
        assertTrue(newLogin.isSuccess());
    }

    @Test
    void testMultipleUserProgressAndMistakeIsolation() {
        String time = "" + System.currentTimeMillis();
        String userA_name = "userA_" + time;
        String userB_name = "userB_" + time;

        authService.register(new RegisterRequest(userA_name, "passA123", "User A", null, UserRole.STUDENT), null);
        authService.register(new RegisterRequest(userB_name, "passB123", "User B", null, UserRole.STUDENT), null);

        User userA = userRepository.findByUsernameIgnoreCase(userA_name).orElseThrow();
        User userB = userRepository.findByUsernameIgnoreCase(userB_name).orElseThrow();

        String topicA = "IsolationTopicA_" + time;
        String topicB = "IsolationTopicB_" + time;

        GeneratedQuestion qA = new GeneratedQuestion("What is A?", "AnsA", List.of("AnsA", "B", "C", "D"), topicA, Difficulty.EASY, QuestionType.MCQ, "Src", "Exp", ApprovalStatus.APPROVED);
        GeneratedQuestion qB = new GeneratedQuestion("What is B?", "AnsB", List.of("AnsB", "X", "Y", "Z"), topicB, Difficulty.EASY, QuestionType.MCQ, "Src", "Exp", ApprovalStatus.APPROVED);
        qA = questionRepository.save(qA);
        qB = questionRepository.save(qB);

        // Student A takes quiz on topicA: answers incorrectly (mistake)
        var sessionA = adaptiveQuizService.startQuizSession(userA_name, topicA);
        String sIdA = (String) sessionA.get("sessionId");

        AnswerSubmissionDto subA = new AnswerSubmissionDto();
        subA.setQuestionId(qA.getId());
        subA.setSelectedAnswer("WrongChoice");
        subA.setResponseTimeSec(10);
        subA.setConfidence(Confidence.HIGH);
        subA.setSessionId(sIdA);
        subA.setStudentName(userA_name);
        QuizFeedbackDto fbA = adaptiveQuizService.submitAnswer(subA);
        assertFalse(fbA.isCorrect());

        // Student B takes quiz on topicB: answers correctly
        var sessionB = adaptiveQuizService.startQuizSession(userB_name, topicB);
        String sIdB = (String) sessionB.get("sessionId");

        AnswerSubmissionDto subB = new AnswerSubmissionDto();
        subB.setQuestionId(qB.getId());
        subB.setSelectedAnswer("AnsB");
        subB.setResponseTimeSec(8);
        subB.setConfidence(Confidence.MEDIUM);
        subB.setSessionId(sIdB);
        subB.setStudentName(userB_name);
        QuizFeedbackDto fbB = adaptiveQuizService.submitAnswer(subB);
        assertTrue(fbB.isCorrect());

        // Verify Dashboard isolation
        DashboardStatsDto statsA = analyticsService.getDashboardStatistics(userA);
        DashboardStatsDto statsB = analyticsService.getDashboardStatistics(userB);

        assertEquals(1, statsA.getTotalQuestionsAttempted());
        assertEquals(0, statsA.getTotalCorrect());
        assertEquals(1, statsA.getTotalIncorrect());
        assertEquals(0.0, statsA.getOverallAccuracy());

        assertEquals(1, statsB.getTotalQuestionsAttempted());
        assertEquals(1, statsB.getTotalCorrect());
        assertEquals(0, statsB.getTotalIncorrect());
        assertEquals(100.0, statsB.getOverallAccuracy());

        // Verify Mistake Isolation: Student A has 1 mistake, Student B has 0 mistakes
        List<QuizAttempt> mistakesA = analyticsService.getMistakes(userA);
        List<QuizAttempt> mistakesB = analyticsService.getMistakes(userB);

        assertEquals(1, mistakesA.size());
        assertEquals("WrongChoice", mistakesA.get(0).getSelectedAnswer());
        assertEquals(0, mistakesB.size(), "Student B must have 0 mistakes!");

        // Verify neither student sees the other's private history
        List<QuizAttempt> historyA = analyticsService.getHistory(userA);
        List<QuizAttempt> historyB = analyticsService.getHistory(userB);
        assertEquals(1, historyA.size());
        assertEquals(topicA, historyA.get(0).getTopic());
        assertEquals(1, historyB.size());
        assertEquals(topicB, historyB.get(0).getTopic());
    }

    @Test
    void testProgressPersistenceAcrossSimulatedSessions() {
        String time = "" + System.currentTimeMillis();
        String username = "persist_" + time;

        authService.register(new RegisterRequest(username, "pass123", "Persistent Student", "p@test.com", UserRole.STUDENT), null);
        User user = userRepository.findByUsernameIgnoreCase(username).orElseThrow();

        // Perform quiz attempt
        GeneratedQuestion q = new GeneratedQuestion("Persistence Question?", "CorrectVal", List.of("CorrectVal", "B", "C", "D"), "Persistence Topic", Difficulty.EASY, QuestionType.MCQ, "S", "E", ApprovalStatus.APPROVED);
        q = questionRepository.save(q);

        var sess = adaptiveQuizService.startQuizSession(username, "Persistence Topic");
        String sId = (String) sess.get("sessionId");

        AnswerSubmissionDto sub = new AnswerSubmissionDto();
        sub.setQuestionId(q.getId());
        sub.setSelectedAnswer("CorrectVal");
        sub.setResponseTimeSec(12);
        sub.setSessionId(sId);
        sub.setStudentName(username);
        adaptiveQuizService.submitAnswer(sub);

        // Word search puzzle
        WordSearchInitResponse wsInit = wordSearchService.createWordSearchSession(null, username);
        assertNotNull(wsInit.getSessionToken());

        // Simulate logout and re-login
        authService.logout(null, null);

        AuthResponse reLogin = authService.login(new AuthRequest(username, "pass123"), null);
        assertTrue(reLogin.isSuccess());

        // Load progress for re-authenticated user
        User reloadedUser = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        DashboardStatsDto reloadedStats = analyticsService.getDashboardStatistics(reloadedUser);

        assertTrue(reloadedStats.getTotalQuestionsAttempted() >= 1, "Previous quiz attempts must be restored");
        assertEquals(100.0, reloadedStats.getOverallAccuracy());

        List<QuizAttempt> history = analyticsService.getHistory(reloadedUser);
        assertFalse(history.isEmpty(), "Quiz history must be preserved");

        var activeSessions = userService.getActiveSessions(reloadedUser);
        assertNotNull(activeSessions, "Active sessions list must be available");
    }

    @Test
    void testRoleBasedAccessControlAndRestrictions() {
        String time = "" + System.currentTimeMillis();
        String studentUser = "student_rbac_" + time;
        String teacherUser = "teacher_rbac_" + time;

        // Student registration without passcode cannot register as TEACHER
        RegisterRequest teacherAttemptWithoutCode = new RegisterRequest("fake_teacher", "pass", "Fake Teacher", null, UserRole.TEACHER);
        AuthResponse badTeacher = authService.register(teacherAttemptWithoutCode, null);
        assertFalse(badTeacher.isSuccess(), "Unauthorized TEACHER registration without code must be rejected");

        // Legitimate teacher registration with secret passcode
        RegisterRequest validTeacherReq = new RegisterRequest(teacherUser, "teach123", "Prof Valid", null, UserRole.TEACHER);
        validTeacherReq.setTeacherPasscode("TEACHER2026");
        AuthResponse validTeacherRes = authService.register(validTeacherReq, null);
        assertTrue(validTeacherRes.isSuccess());
        assertEquals(UserRole.TEACHER, validTeacherRes.getUser().getRole());

        // Student registration
        authService.register(new RegisterRequest(studentUser, "stud123", "Normal Student", null, UserRole.STUDENT), null);

        User student = userRepository.findByUsernameIgnoreCase(studentUser).orElseThrow();
        User teacher = userRepository.findByUsernameIgnoreCase(teacherUser).orElseThrow();

        // 1. Student attempts question approval -> Must be rejected (403 FORBIDDEN)
        GeneratedQuestion pendingQ = new GeneratedQuestion("Review question?", "Ans", List.of("Ans", "B", "C", "D"), "Review Topic", Difficulty.EASY, QuestionType.MCQ, "S", "E", ApprovalStatus.GENERATED);
        pendingQ = questionRepository.save(pendingQ);

        MockHttpServletRequest studentReq = new MockHttpServletRequest();
        String studentToken = "token-student-" + time;
        authService.authenticateAs(student, studentToken);
        studentReq.addHeader("X-Auth-Token", studentToken);

        ResponseEntity<?> studentApproveRes = questionController.approveQuestion(pendingQ.getId(), studentReq);
        assertEquals(HttpStatus.FORBIDDEN, studentApproveRes.getStatusCode(), "Students must NOT be allowed to approve questions");

        // 2. Student attempts question editing -> Must be rejected (403 FORBIDDEN)
        QuestionDto editDto = new QuestionDto(pendingQ);
        editDto.setQuestionText("Unauthorized question edit");
        ResponseEntity<?> studentEditRes = questionController.updateQuestion(pendingQ.getId(), editDto, studentReq);
        assertEquals(HttpStatus.FORBIDDEN, studentEditRes.getStatusCode(), "Students must NOT be allowed to edit questions");

        // 3. Student attempts to access Teacher Dashboard endpoints -> Must be rejected (403 FORBIDDEN)
        ResponseEntity<?> studentViewStudents = teacherController.getTeacherStudents(studentReq);
        assertEquals(HttpStatus.FORBIDDEN, studentViewStudents.getStatusCode(), "Students must NOT be allowed to view teacher student summary list");

        // 4. Teacher approves question -> Must SUCCEED (200 OK)
        MockHttpServletRequest teacherReq = new MockHttpServletRequest();
        String teacherToken = "token-teacher-" + time;
        authService.authenticateAs(teacher, teacherToken);
        teacherReq.addHeader("X-Auth-Token", teacherToken);

        ResponseEntity<?> teacherApproveRes = questionController.approveQuestion(pendingQ.getId(), teacherReq);
        assertEquals(HttpStatus.OK, teacherApproveRes.getStatusCode(), "Teacher must be allowed to approve questions");

        GeneratedQuestion approvedQ = questionRepository.findById(pendingQ.getId()).orElseThrow();
        assertEquals(ApprovalStatus.APPROVED, approvedQ.getApprovalStatus());

        // 5. Teacher views student list -> Must SUCCEED (200 OK)
        ResponseEntity<?> teacherViewStudents = teacherController.getTeacherStudents(teacherReq);
        assertEquals(HttpStatus.OK, teacherViewStudents.getStatusCode());
        List<?> studentList = (List<?>) teacherViewStudents.getBody();
        assertNotNull(studentList);
        assertFalse(studentList.isEmpty());
    }

    @Test
    void testPerformanceReportPdfGeneration() throws Exception {
        User defaultStudent = userRepository.findByUsernameIgnoreCase("student").orElse(null);
        if (defaultStudent == null) {
            defaultStudent = new User("student_pdf", "pass", "PDF Tester", null, UserRole.STUDENT);
            defaultStudent = userRepository.save(defaultStudent);
        }

        byte[] pdfBytes = reportService.generateStudentPerformanceReportPdf(defaultStudent);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF bytes must be generated with real content");

        // Verify PDF Magic Bytes: %PDF-
        String header = new String(Arrays.copyOfRange(pdfBytes, 0, 5));
        assertEquals("%PDF-", header, "File must be a valid PDF document starting with '%PDF-'");
    }

    private byte[] createSamplePdfBytes(String title, String... contentLines) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            doc.addPage(page);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                cs.newLineAtOffset(50, 700);
                cs.showText(title);
                cs.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
                for (String line : contentLines) {
                    cs.newLineAtOffset(0, -20);
                    cs.showText(line);
                }
                cs.endText();
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void testPdfUploadTextExtractionAndQuestionGenerationWorkflow() throws Exception {
        String time = "" + System.currentTimeMillis();
        String teacherUser = "pdf_teacher_" + time;
        RegisterRequest tr = new RegisterRequest(teacherUser, "teach123", "Dr. PDF", null, UserRole.TEACHER);
        tr.setTeacherPasscode("TEACHER2026");
        authService.register(tr, null);
        User teacher = userRepository.findByUsernameIgnoreCase(teacherUser).orElseThrow();

        MockHttpServletRequest teacherReq = new MockHttpServletRequest();
        String token = "token-pdf-" + time;
        authService.authenticateAs(teacher, token);
        teacherReq.addHeader("X-Auth-Token", token);

        // 1. Create real PDF with educational content
        byte[] pdfData = createSamplePdfBytes("Computer Science Fundamentals",
                "An operating system is defined as system software that manages computer hardware and software resources.",
                "Encapsulation is the technique of making the fields in a class private and providing access via public methods.",
                "Virtual memory refers to a memory management capability of an operating system using hardware and software.",
                "Polymorphism allows objects to be treated as instances of their parent class rather than their actual class."
        );

        MockMultipartFile file = new MockMultipartFile("file", "course_materials_" + time + ".pdf", "application/pdf", pdfData);

        // 2. Upload PDF
        ResponseEntity<?> uploadRes = pdfController.uploadPdf(file, teacherReq);
        assertEquals(HttpStatus.OK, uploadRes.getStatusCode(), "PDF upload must succeed");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) uploadRes.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        Long docId = (Long) body.get("documentId");
        assertNotNull(docId);

        // 3. Verify extracted text is stored in database
        PdfDocument savedDoc = pdfDocumentRepository.findById(docId).orElseThrow();
        assertNotNull(savedDoc.getExtractedText(), "Extracted text must be stored in database");
        assertTrue(savedDoc.getExtractedText().contains("operating system"));

        // 4. Trigger Question Generation from documentId endpoint
        ResponseEntity<?> genRes = questionController.generateQuestions(docId, null, teacherReq);
        assertEquals(HttpStatus.OK, genRes.getStatusCode(), "Question generation endpoint must succeed");

        @SuppressWarnings("unchecked")
        Map<String, Object> genBody = (Map<String, Object>) genRes.getBody();
        assertNotNull(genBody);
        assertTrue((Boolean) genBody.get("success"));
        int genCount = (Integer) genBody.get("generatedCount");
        assertTrue(genCount >= 2, "Should generate multiple questions from educational sentences");

        // 5. Retrieve Questions for Document
        ResponseEntity<?> docQuestions = questionController.getQuestionsByDocument(docId);
        assertEquals(HttpStatus.OK, docQuestions.getStatusCode());
        @SuppressWarnings("unchecked")
        List<QuestionDto> list = (List<QuestionDto>) docQuestions.getBody();
        assertNotNull(list);
        assertFalse(list.isEmpty());

        QuestionDto firstQ = list.get(0);
        assertEquals(ApprovalStatus.PENDING, firstQ.getApprovalStatus(), "Generated questions must start with PENDING status");

        // 6. Teacher approves the question
        ResponseEntity<?> approveRes = questionController.approveQuestion(firstQ.getId(), teacherReq);
        assertEquals(HttpStatus.OK, approveRes.getStatusCode());

        GeneratedQuestion afterApprove = questionRepository.findById(firstQ.getId()).orElseThrow();
        assertEquals(ApprovalStatus.APPROVED, afterApprove.getApprovalStatus());

        // 7. Teacher rejects another question
        if (list.size() > 1) {
            QuestionDto secondQ = list.get(1);
            ResponseEntity<?> rejectRes = questionController.rejectQuestion(secondQ.getId(), teacherReq);
            assertEquals(HttpStatus.OK, rejectRes.getStatusCode());
            GeneratedQuestion afterReject = questionRepository.findById(secondQ.getId()).orElseThrow();
            assertEquals(ApprovalStatus.REJECTED, afterReject.getApprovalStatus());
        }
    }

    @Test
    void testInvalidPdfUploadHandling() {
        MockHttpServletRequest teacherReq = new MockHttpServletRequest();
        User teacher = userRepository.findByUsernameIgnoreCase("teacher").orElseThrow();
        String token = "tok-invalid-test";
        authService.authenticateAs(teacher, token);
        teacherReq.addHeader("X-Auth-Token", token);

        // 1. Empty file
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        ResponseEntity<?> emptyRes = pdfController.uploadPdf(emptyFile, teacherReq);
        assertEquals(HttpStatus.BAD_REQUEST, emptyRes.getStatusCode());

        // 2. Non-PDF file
        MockMultipartFile txtFile = new MockMultipartFile("file", "notes.txt", "text/plain", "Hello world text".getBytes());
        ResponseEntity<?> txtRes = pdfController.uploadPdf(txtFile, teacherReq);
        assertEquals(HttpStatus.BAD_REQUEST, txtRes.getStatusCode());

        // 3. Corrupted PDF
        MockMultipartFile corruptFile = new MockMultipartFile("file", "corrupt.pdf", "application/pdf", "corrupted non pdf bytes".getBytes());
        ResponseEntity<?> corruptRes = pdfController.uploadPdf(corruptFile, teacherReq);
        assertTrue(corruptRes.getStatusCode() == HttpStatus.BAD_REQUEST || corruptRes.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testStudentPdfUploadAndPrivateWorkflow() throws Exception {
        long time = System.currentTimeMillis();
        // 1. Create Student 1 and Student 2
        RegisterRequest s1Reg = new RegisterRequest("std_uploader_" + time, "pass123", "Student Uploader " + time, null, UserRole.STUDENT);
        AuthResponse s1Auth = authService.register(s1Reg, null);
        assertNotNull(s1Auth);

        RegisterRequest s2Reg = new RegisterRequest("std_other_" + time, "pass123", "Student Other " + time, null, UserRole.STUDENT);
        AuthResponse s2Auth = authService.register(s2Reg, null);
        assertNotNull(s2Auth);

        MockHttpServletRequest s1Req = new MockHttpServletRequest();
        s1Req.addHeader("X-Auth-Token", s1Auth.getToken());

        MockHttpServletRequest s2Req = new MockHttpServletRequest();
        s2Req.addHeader("X-Auth-Token", s2Auth.getToken());

        // 2. Student 1 uploads a PDF study material
        byte[] pdfData = createSamplePdfBytes("Student Study Notes on Databases",
                "A relational database organizes data into one or more tables of columns and rows.",
                "Normalization is the process of structuring a relational database in accordance with normal forms.",
                "Primary keys are unique identifiers for each record in a database table.",
                "Indexing improves the speed of data retrieval operations on a database table."
        );
        MockMultipartFile file = new MockMultipartFile("file", "student_db_notes_" + time + ".pdf", "application/pdf", pdfData);

        ResponseEntity<?> uploadRes = pdfController.uploadPdf(file, s1Req);
        assertEquals(HttpStatus.OK, uploadRes.getStatusCode(), "Student PDF upload must succeed");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) uploadRes.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        Long docId = (Long) body.get("documentId");
        assertNotNull(docId);

        // Verify document is owned by Student 1
        PdfDocument doc = pdfDocumentRepository.findById(docId).orElseThrow();
        assertEquals(s1Reg.getUsername(), doc.getUploadedBy().getUsername());
        assertEquals(UserRole.STUDENT, doc.getUploadedBy().getRole());

        // 3. Student 1 calls GET /api/pdf/my-materials
        ResponseEntity<?> myMatsRes = pdfController.getMyMaterials(s1Req);
        assertEquals(HttpStatus.OK, myMatsRes.getStatusCode());
        @SuppressWarnings("unchecked")
        List<PdfDocument> myMats = (List<PdfDocument>) myMatsRes.getBody();
        assertNotNull(myMats);
        assertTrue(myMats.stream().anyMatch(m -> docId.equals(m.getId())));

        // Student 2 should NOT see Student 1's materials in their my-materials
        ResponseEntity<?> s2MatsRes = pdfController.getMyMaterials(s2Req);
        assertEquals(HttpStatus.OK, s2MatsRes.getStatusCode());
        @SuppressWarnings("unchecked")
        List<PdfDocument> s2Mats = (List<PdfDocument>) s2MatsRes.getBody();
        assertNotNull(s2Mats);
        assertFalse(s2Mats.stream().anyMatch(m -> docId.equals(m.getId())));

        // 4. Questions generated for student material must be APPROVED immediately
        List<GeneratedQuestion> questions = questionRepository.findByDocumentId(docId);
        assertFalse(questions.isEmpty(), "Questions should have been generated for student study material");
        for (GeneratedQuestion q : questions) {
            assertEquals(ApprovalStatus.APPROVED, q.getApprovalStatus(), "Student questions must be APPROVED directly without teacher review");
        }

        // 5. Public question bank check: Ensure student's private questions do NOT leak into public bank
        List<GeneratedQuestion> sharedQuestions = questionRepository.findSharedApprovedQuestions();
        for (GeneratedQuestion q : sharedQuestions) {
            assertNotEquals(docId, q.getDocument() != null ? q.getDocument().getId() : null, "Student private questions must not leak into shared public question bank");
        }

        // 6. Student 1 starts Word Search on this document
        WordSearchInitResponse wsRes = wordSearchService.createWordSearchSession(docId, s1Reg.getUsername());
        assertNotNull(wsRes);
        assertNotNull(wsRes.getSessionToken());
        assertTrue(wsRes.getTotalQuestions() > 0);

        // 7. Student 1 starts Adaptive Quiz on this document
        Map<String, Object> quizRes = adaptiveQuizService.startQuizSession(s1Reg.getUsername(), null, docId);
        assertNotNull(quizRes);
        assertNotNull(quizRes.get("sessionId"));
        assertNotNull(quizRes.get("question"));

        // 8. Privacy Check: Student 2 attempting to access Student 1's document or generate questions
        ResponseEntity<?> s2GenRes = questionController.generateQuestions(docId, null, s2Req);
        assertEquals(HttpStatus.FORBIDDEN, s2GenRes.getStatusCode(), "Student 2 must be forbidden from generating questions for Student 1's document");

        ResponseEntity<?> s2DocQRes = questionController.getQuestionsByDocument(docId, s2Req);
        assertEquals(HttpStatus.FORBIDDEN, s2DocQRes.getStatusCode(), "Student 2 must be forbidden from viewing Student 1's private questions");
    }
}
