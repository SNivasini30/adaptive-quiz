package com.quizmind.adaptive_quiz.service;

import com.quizmind.adaptive_quiz.model.*;
import com.quizmind.adaptive_quiz.model.dto.*;
import com.quizmind.adaptive_quiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final WordSearchAttemptRepository wordSearchAttemptRepository;
    private final PerformanceAnalyticsService analyticsService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public UserService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       LearningSessionRepository learningSessionRepository,
                       QuizSessionRepository quizSessionRepository,
                       WordSearchAttemptRepository wordSearchAttemptRepository,
                       PerformanceAnalyticsService analyticsService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.quizSessionRepository = quizSessionRepository;
        this.wordSearchAttemptRepository = wordSearchAttemptRepository;
        this.analyticsService = analyticsService;
    }

    public UserProfileDto getProfile(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User not authenticated.");
        }
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseGet(() -> userProfileRepository.save(new UserProfile(user)));
        return new UserProfileDto(user, profile);
    }

    @Transactional
    public UserProfileDto updateProfile(User user, UpdateProfileRequest req) {
        if (user == null) {
            throw new IllegalArgumentException("User not authenticated.");
        }
        if (req != null) {
            if (req.getFullName() != null && !req.getFullName().trim().isEmpty()) {
                user.setFullName(req.getFullName().trim());
            }
            if (req.getEmail() != null) {
                user.setEmail(req.getEmail().trim());
            }
            userRepository.save(user);

            UserProfile profile = userProfileRepository.findByUser(user)
                    .orElseGet(() -> new UserProfile(user));
            if (req.getBio() != null) {
                profile.setBio(req.getBio().trim());
            }
            if (req.getInstitution() != null) {
                profile.setInstitution(req.getInstitution().trim());
            }
            if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
                profile.setAvatarInitial(user.getFullName().trim().substring(0, 1).toUpperCase());
            }
            profile.setUpdatedAt(LocalDateTime.now());
            profile = userProfileRepository.save(profile);

            return new UserProfileDto(user, profile);
        }
        return getProfile(user);
    }

    @Transactional
    public Map<String, Object> changePassword(User user, ChangePasswordRequest req) {
        if (user == null) {
            throw new IllegalArgumentException("User not authenticated.");
        }
        if (req == null || req.getOldPassword() == null || req.getNewPassword() == null) {
            throw new IllegalArgumentException("Old and new passwords are required.");
        }
        if (req.getNewPassword().trim().length() < 4) {
            throw new IllegalArgumentException("New password must be at least 4 characters long.");
        }

        // Verify current password
        if (!passwordEncoder.matches(req.getOldPassword().trim(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword().trim()));
        userRepository.save(user);

        return Map.of("success", true, "message", "Password changed successfully.");
    }

    public Map<String, Object> getUserProgress(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User not authenticated.");
        }
        return analyticsService.getOverallProgress(user);
    }

    public Map<String, Object> getUserHistory(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User not authenticated.");
        }
        List<QuizAttempt> quizHistory = analyticsService.getHistory(user);
        List<WordSearchAttempt> puzzleHistory = wordSearchAttemptRepository.findByUserIdOrderByStartTimeDesc(user.getId());
        if (puzzleHistory.isEmpty()) {
            puzzleHistory = wordSearchAttemptRepository.findByStudentNameOrderByStartTimeDesc(user.getUsername());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("quizAttempts", quizHistory);
        result.put("puzzleAttempts", puzzleHistory);
        return result;
    }

    public List<LearningSessionDto> getActiveSessions(User user) {
        if (user == null) {
            return Collections.emptyList();
        }

        List<LearningSessionDto> active = new ArrayList<>();

        // 1. Check LearningSession table
        List<LearningSession> list = learningSessionRepository.findByUserIdAndCompletedFalseOrderByLastActiveTimeDesc(user.getId());
        for (LearningSession s : list) {
            active.add(new LearningSessionDto(s));
        }

        // 2. Also check incomplete QuizSessions for this user if not already in list
        List<QuizSession> uncompletedQuizzes = quizSessionRepository.findByUserIdAndCompletedFalse(user.getId());
        for (QuizSession qs : uncompletedQuizzes) {
            boolean already = active.stream().anyMatch(a -> a.getSessionId().equals(qs.getSessionId()));
            if (!already) {
                LearningSession ls = new LearningSession(qs.getSessionId(), user, LearningSession.SessionType.QUIZ, qs.getTopic(), qs.getDocument() != null ? qs.getDocument().getId() : null);
                ls.setCompleted(false);
                ls.setTotalQuestions(qs.getTotalAttempted());
                ls.setScore(qs.getTotalScore());
                ls.setStartTime(qs.getStartTime());
                active.add(new LearningSessionDto(ls));
            }
        }

        // 3. Also check incomplete WordSearchAttempts for this user
        List<WordSearchAttempt> uncompletedPuzzles = wordSearchAttemptRepository.findByUserIdAndCompletedFalse(user.getId());
        for (WordSearchAttempt wa : uncompletedPuzzles) {
            boolean already = active.stream().anyMatch(a -> a.getSessionId().equals(wa.getSessionToken()));
            if (!already) {
                LearningSession ls = new LearningSession(wa.getSessionToken(), user, LearningSession.SessionType.WORD_SEARCH, "Word Search", wa.getPuzzle() != null && wa.getPuzzle().getDocument() != null ? wa.getPuzzle().getDocument().getId() : null);
                ls.setCompleted(false);
                ls.setTotalQuestions(wa.getTotalQuestions());
                ls.setCompletedQuestions(wa.getCurrentQuestionIndex());
                ls.setScore(wa.getCorrectCount() * 10);
                ls.setIncorrectAttempts(wa.getIncorrectAttempts());
                ls.setStartTime(wa.getStartTime());
                active.add(new LearningSessionDto(ls));
            }
        }

        return active;
    }

    public List<StudentSummaryDto> getStudentSummariesForTeacher() {
        List<User> students = userRepository.findByRole(UserRole.STUDENT);
        List<StudentSummaryDto> dtos = new ArrayList<>();

        for (User s : students) {
            DashboardStatsDto stats = analyticsService.getDashboardStatistics(s);
            StudentSummaryDto dto = new StudentSummaryDto();
            dto.setId(s.getId());
            dto.setUsername(s.getUsername());
            dto.setFullName(s.getFullName() != null ? s.getFullName() : s.getUsername());
            dto.setEmail(s.getEmail());
            dto.setTotalQuizzes(stats.getTotalQuizzesCompleted());
            dto.setTotalQuestionsAttempted((int) stats.getTotalQuestionsAttempted());
            dto.setOverallAccuracy(stats.getOverallAccuracy());
            dto.setTotalPuzzles(stats.getTotalPuzzlesCompleted());
            dto.setStrongestTopic(stats.getStrongestTopic());
            dto.setWeakestTopic(stats.getWeakestTopic());
            dto.setLastActive(s.getLastLoginAt() != null ? s.getLastLoginAt() : s.getCreatedAt());
            dtos.add(dto);
        }

        return dtos;
    }

    public Map<String, Object> getStudentPerformanceForTeacher(Long studentUserId) {
        User student = userRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentUserId));

        DashboardStatsDto stats = analyticsService.getDashboardStatistics(student);
        List<TopicPerformanceDto> topics = analyticsService.getTopicPerformances(student);
        List<QuizAttempt> history = analyticsService.getHistory(student);
        List<QuizAttempt> mistakes = analyticsService.getMistakes(student);

        Map<String, Object> res = new HashMap<>();
        res.put("student", new UserProfileDto(student));
        res.put("stats", stats);
        res.put("topics", topics);
        res.put("history", history);
        res.put("mistakes", mistakes);
        return res;
    }
}
