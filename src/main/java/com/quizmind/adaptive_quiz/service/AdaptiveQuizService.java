package com.quizmind.adaptive_quiz.service;

import com.quizmind.adaptive_quiz.model.*;
import com.quizmind.adaptive_quiz.model.dto.AnswerSubmissionDto;
import com.quizmind.adaptive_quiz.model.dto.QuestionDto;
import com.quizmind.adaptive_quiz.model.dto.QuizFeedbackDto;
import com.quizmind.adaptive_quiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdaptiveQuizService {

    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final StudentTopicProgressRepository progressRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final PdfDocumentRepository pdfDocumentRepository;
    private final UserRepository userRepository;
    private final LearningSessionRepository learningSessionRepository;

    @Autowired
    public AdaptiveQuizService(QuestionRepository questionRepository,
                               QuizAttemptRepository quizAttemptRepository,
                               StudentTopicProgressRepository progressRepository,
                               QuizSessionRepository quizSessionRepository,
                               PdfDocumentRepository pdfDocumentRepository,
                               UserRepository userRepository,
                               LearningSessionRepository learningSessionRepository) {
        this.questionRepository = questionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.progressRepository = progressRepository;
        this.quizSessionRepository = quizSessionRepository;
        this.pdfDocumentRepository = pdfDocumentRepository;
        this.userRepository = userRepository;
        this.learningSessionRepository = learningSessionRepository;
    }

    @Transactional
    public Map<String, Object> startQuizSession(String studentName, String requestedTopic) {
        return startQuizSession(studentName, requestedTopic, null);
    }

    @Transactional
    public Map<String, Object> startQuizSession(String studentName, String requestedTopic, Long documentId) {
        String effectiveStudent = (studentName != null && !studentName.trim().isEmpty()) ? studentName.trim() : "Student";
        String sessionId = UUID.randomUUID().toString();

        User user = userRepository.findByUsernameIgnoreCase(effectiveStudent).orElse(null);

        PdfDocument document = null;
        if (documentId != null) {
            document = pdfDocumentRepository.findById(documentId).orElse(null);
        }

        QuizSession session = new QuizSession(sessionId, effectiveStudent, requestedTopic, document);
        if (user != null) {
            session.setUser(user);
        }
        quizSessionRepository.save(session);

        if (user != null) {
            LearningSession ls = new LearningSession(sessionId, user, LearningSession.SessionType.QUIZ, requestedTopic != null ? requestedTopic : "Adaptive", documentId);
            learningSessionRepository.save(ls);
        }

        QuestionDto firstQuestion = getNextAdaptiveQuestion(effectiveStudent, requestedTopic, sessionId, documentId);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("studentName", effectiveStudent);
        response.put("topic", requestedTopic);
        response.put("documentId", documentId);
        response.put("question", firstQuestion);
        return response;
    }

    @Transactional(readOnly = true)
    public QuestionDto getNextAdaptiveQuestion(String studentName, String topic, String sessionId) {
        return getNextAdaptiveQuestion(studentName, topic, sessionId, null);
    }

    @Transactional(readOnly = true)
    public QuestionDto getNextAdaptiveQuestion(String studentName, String topic, String sessionId, Long documentId) {
        Long effectiveDocId = documentId;
        if (effectiveDocId == null && sessionId != null) {
            Optional<QuizSession> optSession = quizSessionRepository.findBySessionId(sessionId);
            if (optSession.isPresent() && optSession.get().getDocument() != null) {
                effectiveDocId = optSession.get().getDocument().getId();
            }
        }

        Set<Long> attemptedIds = new HashSet<>();
        if (sessionId != null) {
            List<QuizAttempt> sessionAttempts = quizAttemptRepository.findBySessionId(sessionId);
            for (QuizAttempt a : sessionAttempts) {
                if (a.getQuestion() != null) {
                    attemptedIds.add(a.getQuestion().getId());
                }
            }
        }

        // A. If effectiveDocId is specified, serve questions exclusively from that specific document
        if (effectiveDocId != null) {
            PdfDocument doc = pdfDocumentRepository.findById(effectiveDocId).orElse(null);
            if (doc == null) {
                return null;
            }
            if (doc.getUploadedBy() != null && doc.getUploadedBy().getRole() == com.quizmind.adaptive_quiz.model.UserRole.STUDENT) {
                if (studentName != null && !studentName.equalsIgnoreCase(doc.getUploadedBy().getUsername())) {
                    return null; // unauthorized access to another student's document
                }
            }

            List<GeneratedQuestion> docQuestions = questionRepository.findByDocumentId(effectiveDocId);
            // All questions from the document are playable immediately without requiring approval
            docQuestions = docQuestions.stream()
                    .filter(q -> q.getApprovalStatus() != ApprovalStatus.REJECTED)
                    .toList();
            if (docQuestions.isEmpty()) {
                docQuestions = questionRepository.findByDocumentId(effectiveDocId);
            }

            List<GeneratedQuestion> unattempted = docQuestions.stream().filter(q -> !attemptedIds.contains(q.getId())).toList();
            if (unattempted.isEmpty()) {
                return null; // All questions from this document have been attempted
            }

            String docTopic = !unattempted.isEmpty() ? unattempted.get(0).getTopic() : "General";
            User user = (studentName != null) ? userRepository.findByUsernameIgnoreCase(studentName.trim()).orElse(null) : null;
            StudentTopicProgress progress;
            if (user != null) {
                progress = progressRepository.findByUserIdAndTopic(user.getId(), docTopic)
                        .orElseGet(() -> progressRepository.save(new StudentTopicProgress(user, docTopic)));
            } else {
                progress = progressRepository.findByStudentNameAndTopic(studentName, docTopic)
                        .orElseGet(() -> progressRepository.save(new StudentTopicProgress(studentName, docTopic)));
            }
            Difficulty targetDiff = progress.getCurrentDifficulty();

            GeneratedQuestion selected = unattempted.stream().filter(q -> q.getDifficulty() == targetDiff).findFirst()
                    .orElse(unattempted.get(0));

            return new QuestionDto(selected);
        }

        // B. Global / Shared Question Pool (documentId == null)
        String effectiveTopic = topic;
        if (effectiveTopic == null || effectiveTopic.trim().isEmpty() || effectiveTopic.equalsIgnoreCase("ALL")) {
            List<String> topics = questionRepository.findDistinctApprovedTopics();
            if (topics.isEmpty()) {
                topics = questionRepository.findAllDistinctTopics();
            }
            if (!topics.isEmpty()) {
                effectiveTopic = topics.get(new Random().nextInt(topics.size()));
            } else {
                effectiveTopic = "General";
            }
        }

        final String topicForProgress = effectiveTopic;
        User user = (studentName != null) ? userRepository.findByUsernameIgnoreCase(studentName.trim()).orElse(null) : null;

        StudentTopicProgress progress;
        if (user != null) {
            progress = progressRepository.findByUserIdAndTopic(user.getId(), topicForProgress)
                    .orElseGet(() -> {
                        StudentTopicProgress p = new StudentTopicProgress(user, topicForProgress);
                        return progressRepository.save(p);
                    });
        } else {
            progress = progressRepository.findByStudentNameAndTopic(studentName, topicForProgress)
                    .orElseGet(() -> {
                        StudentTopicProgress p = new StudentTopicProgress(studentName, topicForProgress);
                        return progressRepository.save(p);
                    });
        }

        Difficulty targetDiff = progress.getCurrentDifficulty();

        // 2. Try finding approved question for (topic, targetDiff)
        List<GeneratedQuestion> candidates = questionRepository.findByApprovalStatusAndTopicAndDifficulty(
                ApprovalStatus.APPROVED, effectiveTopic, targetDiff
        );
        candidates = candidates.stream().filter(q -> q.getDocument() == null || q.getDocument().getUploadedBy() == null || q.getDocument().getUploadedBy().getRole() == com.quizmind.adaptive_quiz.model.UserRole.TEACHER).toList();

        GeneratedQuestion selected = selectUnattempted(candidates, attemptedIds);

        // 3. Fallback: try any difficulty in this topic
        if (selected == null) {
            List<GeneratedQuestion> topicQuestions = questionRepository.findByApprovalStatusAndTopic(
                    ApprovalStatus.APPROVED, effectiveTopic
            );
            topicQuestions = topicQuestions.stream().filter(q -> q.getDocument() == null || q.getDocument().getUploadedBy() == null || q.getDocument().getUploadedBy().getRole() == com.quizmind.adaptive_quiz.model.UserRole.TEACHER).toList();
            selected = selectUnattempted(topicQuestions, attemptedIds);
        }

        // 4. Fallback: try any approved question across all topics
        if (selected == null) {
            List<GeneratedQuestion> allApproved = questionRepository.findSharedApprovedQuestions();
            selected = selectUnattempted(allApproved, attemptedIds);
        }

        if (selected == null) {
            return null;
        }

        return new QuestionDto(selected);
    }

    private GeneratedQuestion selectUnattempted(List<GeneratedQuestion> questions, Set<Long> attemptedIds) {
        if (questions == null || questions.isEmpty()) {
            return null;
        }
        for (GeneratedQuestion q : questions) {
            if (!attemptedIds.contains(q.getId())) {
                return q;
            }
        }
        return null;
    }

    @Transactional
    public QuizFeedbackDto submitAnswer(AnswerSubmissionDto submission) {
        if (submission == null || submission.getQuestionId() == null) {
            throw new IllegalArgumentException("Invalid submission: questionId is required.");
        }

        GeneratedQuestion question = questionRepository.findById(submission.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + submission.getQuestionId()));

        String studentName = (submission.getStudentName() != null && !submission.getStudentName().trim().isEmpty())
                ? submission.getStudentName().trim() : "Student";

        User user = userRepository.findByUsernameIgnoreCase(studentName).orElse(null);

        // Prevent duplicate submission in same session
        if (submission.getSessionId() != null) {
            List<QuizAttempt> existing = quizAttemptRepository.findBySessionId(submission.getSessionId());
            boolean alreadyAnswered = existing.stream().anyMatch(a -> a.getQuestion() != null && a.getQuestion().getId().equals(question.getId()));
            if (alreadyAnswered) {
                throw new IllegalStateException("Question already answered in this session.");
            }
        }

        // Check correctness
        boolean isCorrect = checkAnswer(question, submission.getSelectedAnswer());

        // Update Topic Progress & Adaptive difficulty
        StudentTopicProgress progress;
        if (user != null) {
            progress = progressRepository.findByUserIdAndTopic(user.getId(), question.getTopic())
                    .orElseGet(() -> new StudentTopicProgress(user, question.getTopic()));
        } else {
            progress = progressRepository.findByStudentNameAndTopic(studentName, question.getTopic())
                    .orElseGet(() -> new StudentTopicProgress(studentName, question.getTopic()));
        }

        Difficulty prevDifficulty = progress.getCurrentDifficulty();
        Difficulty newDifficulty = prevDifficulty;

        progress.setTotalAttempts(progress.getTotalAttempts() + 1);

        int points = 0;
        if (isCorrect) {
            progress.setCorrectAttempts(progress.getCorrectAttempts() + 1);
            int streak = progress.getConsecutiveCorrect() + 1;
            progress.setConsecutiveCorrect(streak);
            progress.setConsecutiveIncorrect(0);

            // Deterministic Rule: Increase difficulty after three consecutive correct answers
            if (streak >= 3) {
                if (prevDifficulty == Difficulty.EASY) {
                    newDifficulty = Difficulty.MEDIUM;
                } else if (prevDifficulty == Difficulty.MEDIUM) {
                    newDifficulty = Difficulty.HARD;
                }
                progress.setCurrentDifficulty(newDifficulty);
                progress.setConsecutiveCorrect(0); // reset streak tracker for next tier
            }

            points = switch (prevDifficulty) {
                case HARD -> 15;
                case MEDIUM -> 10;
                case EASY -> 5;
            };
        } else {
            progress.setConsecutiveCorrect(0);
            int errors = progress.getConsecutiveIncorrect() + 1;
            progress.setConsecutiveIncorrect(errors);

            // Deterministic Rule: Decrease difficulty after two consecutive incorrect answers
            if (errors >= 2) {
                if (prevDifficulty == Difficulty.HARD) {
                    newDifficulty = Difficulty.MEDIUM;
                } else if (prevDifficulty == Difficulty.MEDIUM) {
                    newDifficulty = Difficulty.EASY;
                }
                progress.setCurrentDifficulty(newDifficulty);
                progress.setConsecutiveIncorrect(0);
            }
        }

        progress.setLastUpdated(LocalDateTime.now());
        if (user != null) {
            progress.setUser(user);
        }
        progressRepository.save(progress);

        // Save Attempt Record
        QuizAttempt attempt = new QuizAttempt(
                studentName,
                question,
                submission.getSelectedAnswer(),
                isCorrect,
                question.getTopic(),
                prevDifficulty,
                Math.max(1, submission.getResponseTimeSec()),
                submission.getConfidence(),
                submission.getSessionId()
        );
        if (user != null) {
            attempt.setUser(user);
        }
        quizAttemptRepository.save(attempt);

        // Update QuizSession if exists
        if (submission.getSessionId() != null) {
            quizSessionRepository.findBySessionId(submission.getSessionId()).ifPresent(s -> {
                s.setTotalAttempted(s.getTotalAttempted() + 1);
                if (isCorrect) {
                    s.setCorrectCount(s.getCorrectCount() + 1);
                }
                s.setTotalScore(s.getTotalScore() + (isCorrect ? 10 : 0));
                if (user != null && s.getUser() == null) {
                    s.setUser(user);
                }
                quizSessionRepository.save(s);
            });

            // Update LearningSession
            learningSessionRepository.findBySessionId(submission.getSessionId()).ifPresent(ls -> {
                ls.setCompletedQuestions(ls.getCompletedQuestions() + 1);
                ls.setLastActiveTime(LocalDateTime.now());
                if (isCorrect) {
                    ls.setScore(ls.getScore() + 10);
                } else {
                    ls.setIncorrectAttempts(ls.getIncorrectAttempts() + 1);
                }
                ls.setDurationSeconds(ls.getDurationSeconds() + Math.max(1, submission.getResponseTimeSec()));
                learningSessionRepository.save(ls);
            });
        }

        // Calculate Feedback details
        QuizFeedbackDto feedback = new QuizFeedbackDto();
        feedback.setCorrect(isCorrect);
        feedback.setSelectedAnswer(submission.getSelectedAnswer());
        feedback.setCorrectAnswer(question.getCorrectAnswer());
        feedback.setExplanation(question.getExplanation() != null ? question.getExplanation() : question.getSourceSentence());
        feedback.setTopic(question.getTopic());
        feedback.setPreviousDifficulty(prevDifficulty);
        feedback.setNewDifficulty(newDifficulty);
        feedback.setDifficultyChanged(prevDifficulty != newDifficulty);
        feedback.setConsecutiveCorrect(progress.getConsecutiveCorrect());
        feedback.setPointsEarned(points);
        feedback.setCurrentStreak(progress.getConsecutiveCorrect());

        // Metacognitive checks
        if (!isCorrect && submission.getConfidence() == Confidence.HIGH) {
            feedback.setMisconceptionAlert(true);
        }
        if (isCorrect && submission.getConfidence() == Confidence.LOW) {
            feedback.setConfidenceBoost(true);
        }

        // Fetch Next Adaptive Question (preserving documentId if session is tied to a specific study material)
        Long docId = null;
        if (submission.getSessionId() != null) {
            QuizSession quizSess = quizSessionRepository.findBySessionId(submission.getSessionId()).orElse(null);
            if (quizSess != null && quizSess.getDocument() != null) {
                docId = quizSess.getDocument().getId();
            }
        }
        QuestionDto nextQ = getNextAdaptiveQuestion(studentName, question.getTopic(), submission.getSessionId(), docId);
        feedback.setNextQuestion(nextQ);
        feedback.setQuizCompleted(nextQ == null);

        if (nextQ == null && submission.getSessionId() != null) {
            quizSessionRepository.findBySessionId(submission.getSessionId()).ifPresent(s -> {
                s.setCompleted(true);
                s.setEndTime(LocalDateTime.now());
                quizSessionRepository.save(s);
            });
            learningSessionRepository.findBySessionId(submission.getSessionId()).ifPresent(ls -> {
                ls.setCompleted(true);
                ls.setEndTime(LocalDateTime.now());
                learningSessionRepository.save(ls);
            });
        }

        return feedback;
    }

    @Transactional
    public Map<String, Object> getSessionResult(String sessionId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findBySessionId(sessionId);
        int total = attempts.size();
        long correct = attempts.stream().filter(QuizAttempt::isCorrect).count();
        double accuracy = total > 0 ? (correct * 100.0 / total) : 0.0;
        double avgTime = attempts.stream().mapToInt(QuizAttempt::getResponseTimeSec).average().orElse(0.0);

        quizSessionRepository.findBySessionId(sessionId).ifPresent(s -> {
            s.setCompleted(true);
            s.setEndTime(LocalDateTime.now());
            quizSessionRepository.save(s);
        });

        learningSessionRepository.findBySessionId(sessionId).ifPresent(ls -> {
            ls.setCompleted(true);
            ls.setEndTime(LocalDateTime.now());
            learningSessionRepository.save(ls);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("totalQuestions", total);
        result.put("correctAnswers", correct);
        result.put("incorrectAnswers", total - correct);
        result.put("accuracy", Math.round(accuracy * 10.0) / 10.0);
        result.put("averageResponseTimeSec", Math.round(avgTime * 10.0) / 10.0);
        result.put("attempts", attempts);
        return result;
    }

    private boolean checkAnswer(GeneratedQuestion question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }
        String cleanUser = userAnswer.trim();
        String cleanCorrect = question.getCorrectAnswer().trim();

        if (cleanUser.equalsIgnoreCase(cleanCorrect)) {
            return true;
        }

        // Strip non-alphanumeric chars
        String normUser = cleanUser.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String normCorrect = cleanCorrect.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return normUser.equals(normCorrect);
    }
}
