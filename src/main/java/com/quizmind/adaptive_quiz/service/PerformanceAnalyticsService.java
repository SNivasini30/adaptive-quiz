package com.quizmind.adaptive_quiz.service;

import com.quizmind.adaptive_quiz.model.QuizAttempt;
import com.quizmind.adaptive_quiz.model.QuizSession;
import com.quizmind.adaptive_quiz.model.StudentTopicProgress;
import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.WordSearchAttempt;
import com.quizmind.adaptive_quiz.model.dto.DashboardStatsDto;
import com.quizmind.adaptive_quiz.model.dto.TopicPerformanceDto;
import com.quizmind.adaptive_quiz.repository.QuizAttemptRepository;
import com.quizmind.adaptive_quiz.repository.QuizSessionRepository;
import com.quizmind.adaptive_quiz.repository.StudentTopicProgressRepository;
import com.quizmind.adaptive_quiz.repository.UserRepository;
import com.quizmind.adaptive_quiz.repository.WordSearchAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PerformanceAnalyticsService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final StudentTopicProgressRepository progressRepository;
    private final WordSearchAttemptRepository wordSearchAttemptRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final UserRepository userRepository;

    @Autowired
    public PerformanceAnalyticsService(QuizAttemptRepository quizAttemptRepository,
                                       StudentTopicProgressRepository progressRepository,
                                       WordSearchAttemptRepository wordSearchAttemptRepository,
                                       QuizSessionRepository quizSessionRepository,
                                       UserRepository userRepository) {
        this.quizAttemptRepository = quizAttemptRepository;
        this.progressRepository = progressRepository;
        this.wordSearchAttemptRepository = wordSearchAttemptRepository;
        this.quizSessionRepository = quizSessionRepository;
        this.userRepository = userRepository;
    }

    public DashboardStatsDto getDashboardStatistics(String studentName) {
        User user = null;
        if (studentName != null && !studentName.trim().isEmpty()) {
            user = userRepository.findByUsernameIgnoreCase(studentName.trim()).orElse(null);
        }
        if (user != null) {
            return getDashboardStatistics(user);
        }
        return getDashboardStatisticsInternal(null, studentName);
    }

    public DashboardStatsDto getDashboardStatistics(User user) {
        return getDashboardStatisticsInternal(user, user != null ? user.getUsername() : "Student");
    }

    private DashboardStatsDto getDashboardStatisticsInternal(User user, String fallbackName) {
        String studentName = user != null ? user.getUsername() : (fallbackName != null ? fallbackName : "Student");
        String displayName = user != null ? (user.getFullName() != null ? user.getFullName() : user.getUsername()) : studentName;

        List<QuizAttempt> userAttempts;
        List<StudentTopicProgress> userProgress;
        List<QuizSession> userSessions;
        List<WordSearchAttempt> userPuzzles;

        if (user != null) {
            userAttempts = quizAttemptRepository.findByUserIdOrderByAttemptTimestampDesc(user.getId());
            userProgress = progressRepository.findByUserId(user.getId());
            userSessions = quizSessionRepository.findByUserIdOrderByStartTimeDesc(user.getId());
            userPuzzles = wordSearchAttemptRepository.findByUserIdOrderByStartTimeDesc(user.getId());

            // If empty by user_id, fallback to matching username for seeded/legacy data
            if (userAttempts.isEmpty() && studentName != null) {
                userAttempts = quizAttemptRepository.findByStudentNameOrderByAttemptTimestampDesc(studentName);
            }
            if (userProgress.isEmpty() && studentName != null) {
                userProgress = progressRepository.findByStudentName(studentName);
            }
            if (userPuzzles.isEmpty() && studentName != null) {
                userPuzzles = wordSearchAttemptRepository.findByStudentNameOrderByStartTimeDesc(studentName);
            }
        } else {
            userAttempts = quizAttemptRepository.findByStudentNameOrderByAttemptTimestampDesc(studentName);
            userProgress = progressRepository.findByStudentName(studentName);
            userSessions = Collections.emptyList();
            userPuzzles = wordSearchAttemptRepository.findByStudentNameOrderByStartTimeDesc(studentName);
        }

        long totalAttempts = userAttempts.size();
        long correctAttempts = userAttempts.stream().filter(QuizAttempt::isCorrect).count();
        long incorrectAttempts = totalAttempts - correctAttempts;

        double overallAccuracy = totalAttempts > 0 ? ((double) correctAttempts / totalAttempts) * 100.0 : 0.0;

        double totalTime = 0.0;
        for (QuizAttempt a : userAttempts) {
            totalTime += a.getResponseTimeSec();
        }
        double avgResponseTime = totalAttempts > 0 ? Math.round((totalTime / totalAttempts) * 10.0) / 10.0 : 0.0;

        Map<String, int[]> topicCounts = new HashMap<>(); // topic -> [total, correct]
        for (QuizAttempt a : userAttempts) {
            String topic = a.getTopic() != null ? a.getTopic() : "General";
            topicCounts.putIfAbsent(topic, new int[]{0, 0});
            topicCounts.get(topic)[0]++;
            if (a.isCorrect()) {
                topicCounts.get(topic)[1]++;
            }
        }

        Map<String, String> topicDiffs = new HashMap<>();
        for (StudentTopicProgress p : userProgress) {
            topicDiffs.put(p.getTopic(), p.getCurrentDifficulty().name());
        }

        List<TopicPerformanceDto> topicDtos = new ArrayList<>();
        List<String> weakTopics = new ArrayList<>();
        List<String> strongTopics = new ArrayList<>();
        String bestTopic = null;
        double bestAcc = -1.0;
        String worstTopic = null;
        double worstAcc = 101.0;

        for (Map.Entry<String, int[]> entry : topicCounts.entrySet()) {
            String topic = entry.getKey();
            int total = entry.getValue()[0];
            int correct = entry.getValue()[1];
            double acc = total > 0 ? Math.round(((double) correct / total) * 100.0 * 10.0) / 10.0 : 0.0;
            String diff = topicDiffs.getOrDefault(topic, "EASY");

            boolean isWeak = acc < 60.0;
            boolean isStrong = acc >= 75.0 && total >= 2;

            if (isWeak) weakTopics.add(topic);
            if (isStrong) strongTopics.add(topic);

            if (acc > bestAcc && total >= 2) {
                bestAcc = acc;
                bestTopic = topic;
            }
            if (acc < worstAcc && total >= 2) {
                worstAcc = acc;
                worstTopic = topic;
            }

            topicDtos.add(new TopicPerformanceDto(topic, total, correct, acc, diff, isWeak, isStrong));
        }

        if (bestTopic == null && !topicDtos.isEmpty()) {
            bestTopic = topicDtos.get(0).getTopic();
        }
        if (worstTopic == null && !topicDtos.isEmpty()) {
            worstTopic = topicDtos.get(topicDtos.size() - 1).getTopic();
        }

        topicDtos.sort((a, b) -> Integer.compare(b.getTotalQuestions(), a.getTotalQuestions()));

        int completedQuizzes = (int) userSessions.stream().filter(QuizSession::isCompleted).count();
        if (completedQuizzes == 0 && totalAttempts > 0) {
            completedQuizzes = (int) Math.max(1, totalAttempts / 3);
        }

        int completedPuzzles = (int) userPuzzles.stream().filter(WordSearchAttempt::isCompleted).count();
        int totalWordsFound = userPuzzles.stream().mapToInt(WordSearchAttempt::getCorrectCount).sum();
        int totalIncorrectWords = userPuzzles.stream().mapToInt(WordSearchAttempt::getIncorrectAttempts).sum();

        DashboardStatsDto dto = new DashboardStatsDto();
        dto.setStudentName(studentName);
        dto.setFullName(displayName);
        dto.setTotalQuizzesCompleted(completedQuizzes);
        dto.setTotalQuestionsAttempted(totalAttempts);
        dto.setOverallAccuracy(Math.round(overallAccuracy * 10.0) / 10.0);
        dto.setAverageResponseTimeSec(avgResponseTime);
        dto.setTotalTopicsPracticed(topicCounts.size());
        dto.setTotalPuzzlesCompleted(completedPuzzles);
        dto.setTotalCorrectWords(totalWordsFound);
        dto.setTotalIncorrectAttempts(totalIncorrectWords + (int) incorrectAttempts);
        dto.setStrongestTopic(bestTopic != null ? bestTopic : "None yet");
        dto.setWeakestTopic(worstTopic != null ? worstTopic : "None yet");
        dto.setCorrectAnswersCount(correctAttempts);
        dto.setIncorrectAnswersCount(incorrectAttempts);
        dto.setTopicPerformances(topicDtos);
        dto.setWeakTopics(weakTopics);
        dto.setStrongTopics(strongTopics);
        dto.setRecentActivity(userAttempts.size() > 20 ? userAttempts.subList(0, 20) : userAttempts);

        return dto;
    }

    public List<TopicPerformanceDto> getTopicPerformances(User user) {
        return getDashboardStatistics(user).getTopicPerformances();
    }

    public List<TopicPerformanceDto> getTopicPerformances(String studentName) {
        return getDashboardStatistics(studentName).getTopicPerformances();
    }

    public List<QuizAttempt> getRecentActivity(User user) {
        if (user == null) return Collections.emptyList();
        List<QuizAttempt> list = quizAttemptRepository.findByUserIdOrderByAttemptTimestampDesc(user.getId());
        if (list.isEmpty()) {
            list = quizAttemptRepository.findByStudentNameOrderByAttemptTimestampDesc(user.getUsername());
        }
        return list.size() > 20 ? list.subList(0, 20) : list;
    }

    public List<QuizAttempt> getRecentActivity(String studentName) {
        User user = studentName != null ? userRepository.findByUsernameIgnoreCase(studentName.trim()).orElse(null) : null;
        if (user != null) {
            return getRecentActivity(user);
        }
        List<QuizAttempt> list = quizAttemptRepository.findByStudentNameOrderByAttemptTimestampDesc(studentName);
        return list.size() > 20 ? list.subList(0, 20) : list;
    }

    public List<QuizAttempt> getMistakes(User user) {
        if (user == null) return Collections.emptyList();
        List<QuizAttempt> list = quizAttemptRepository.findByUserIdAndCorrectFalseOrderByAttemptTimestampDesc(user.getId());
        if (list.isEmpty()) {
            List<QuizAttempt> byName = quizAttemptRepository.findByStudentNameOrderByAttemptTimestampDesc(user.getUsername());
            list = byName.stream().filter(a -> !a.isCorrect()).toList();
        }
        return list;
    }

    public List<QuizAttempt> getMistakes(String studentName) {
        User user = studentName != null ? userRepository.findByUsernameIgnoreCase(studentName.trim()).orElse(null) : null;
        if (user != null) {
            return getMistakes(user);
        }
        List<QuizAttempt> byName = quizAttemptRepository.findByStudentNameOrderByAttemptTimestampDesc(studentName);
        if (byName.isEmpty() && "Student".equalsIgnoreCase(studentName)) {
            return quizAttemptRepository.findByCorrectFalseOrderByAttemptTimestampDesc();
        }
        return byName.stream().filter(a -> !a.isCorrect()).toList();
    }

    public Map<String, Object> getMistakesByTopic(User user) {
        List<QuizAttempt> mistakes = getMistakes(user);
        Map<String, List<QuizAttempt>> grouped = new LinkedHashMap<>();
        for (QuizAttempt m : mistakes) {
            String topic = m.getTopic() != null ? m.getTopic() : "General";
            grouped.computeIfAbsent(topic, k -> new ArrayList<>()).add(m);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalMistakes", mistakes.size());
        result.put("byTopic", grouped);
        return result;
    }

    public Map<String, Object> getMistakesByTopic(String studentName) {
        User user = studentName != null ? userRepository.findByUsernameIgnoreCase(studentName.trim()).orElse(null) : null;
        if (user != null) {
            return getMistakesByTopic(user);
        }
        List<QuizAttempt> mistakes = getMistakes(studentName);
        Map<String, List<QuizAttempt>> grouped = new LinkedHashMap<>();
        for (QuizAttempt m : mistakes) {
            String topic = m.getTopic() != null ? m.getTopic() : "General";
            grouped.computeIfAbsent(topic, k -> new ArrayList<>()).add(m);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalMistakes", mistakes.size());
        result.put("byTopic", grouped);
        return result;
    }

    public Map<String, Object> getOverallProgress(User user) {
        if (user == null) return Collections.emptyMap();
        DashboardStatsDto stats = getDashboardStatistics(user);
        List<WordSearchAttempt> puzzleAttempts = wordSearchAttemptRepository.findByUserIdOrderByStartTimeDesc(user.getId());
        if (puzzleAttempts.isEmpty()) {
            puzzleAttempts = wordSearchAttemptRepository.findByStudentNameOrderByStartTimeDesc(user.getUsername());
        }

        Map<String, Object> progress = new HashMap<>();
        progress.put("userId", user.getId());
        progress.put("username", user.getUsername());
        progress.put("studentName", user.getFullName() != null ? user.getFullName() : user.getUsername());
        progress.put("totalQuizzesAttempted", stats.getTotalQuestionsAttempted());
        progress.put("overallAccuracy", stats.getOverallAccuracy());
        progress.put("averageResponseTimeSec", stats.getAverageResponseTimeSec());
        progress.put("topicsPracticed", stats.getTotalTopicsPracticed());
        progress.put("weakTopics", stats.getWeakTopics());
        progress.put("strongTopics", stats.getStrongTopics());
        progress.put("puzzleAttemptsCount", puzzleAttempts.size());
        progress.put("puzzlesCompleted", puzzleAttempts.stream().filter(WordSearchAttempt::isCompleted).count());
        return progress;
    }

    public Map<String, Object> getOverallProgress(String studentName) {
        User user = studentName != null ? userRepository.findByUsernameIgnoreCase(studentName.trim()).orElse(null) : null;
        if (user != null) {
            return getOverallProgress(user);
        }
        String effectiveStudent = (studentName != null && !studentName.trim().isEmpty()) ? studentName.trim() : "Student";
        DashboardStatsDto stats = getDashboardStatistics(effectiveStudent);
        List<WordSearchAttempt> puzzleAttempts = wordSearchAttemptRepository.findByStudentNameOrderByStartTimeDesc(effectiveStudent);

        Map<String, Object> progress = new HashMap<>();
        progress.put("studentName", effectiveStudent);
        progress.put("totalQuizzesAttempted", stats.getTotalQuestionsAttempted());
        progress.put("overallAccuracy", stats.getOverallAccuracy());
        progress.put("averageResponseTimeSec", stats.getAverageResponseTimeSec());
        progress.put("topicsPracticed", stats.getTotalTopicsPracticed());
        progress.put("weakTopics", stats.getWeakTopics());
        progress.put("strongTopics", stats.getStrongTopics());
        progress.put("puzzleAttemptsCount", puzzleAttempts.size());
        progress.put("puzzlesCompleted", puzzleAttempts.stream().filter(WordSearchAttempt::isCompleted).count());
        return progress;
    }

    public List<QuizAttempt> getHistory(User user) {
        if (user == null) return Collections.emptyList();
        List<QuizAttempt> list = quizAttemptRepository.findByUserIdOrderByAttemptTimestampDesc(user.getId());
        if (list.isEmpty()) {
            list = quizAttemptRepository.findByStudentNameOrderByAttemptTimestampDesc(user.getUsername());
        }
        return list;
    }

    public List<QuizAttempt> getHistory(String studentName) {
        User user = studentName != null ? userRepository.findByUsernameIgnoreCase(studentName.trim()).orElse(null) : null;
        if (user != null) {
            return getHistory(user);
        }
        return quizAttemptRepository.findByStudentNameOrderByAttemptTimestampDesc(studentName);
    }
}
