package com.quizmind.adaptive_quiz.service;

import com.quizmind.adaptive_quiz.model.QuizAttempt;
import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.dto.DashboardStatsDto;
import com.quizmind.adaptive_quiz.model.dto.TopicPerformanceDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private final PerformanceAnalyticsService analyticsService;

    @Autowired
    public ReportService(PerformanceAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public byte[] generateStudentPerformanceReportPdf(User student) throws IOException {
        DashboardStatsDto stats = analyticsService.getDashboardStatistics(student);
        List<TopicPerformanceDto> topics = analyticsService.getTopicPerformances(student);
        List<QuizAttempt> history = analyticsService.getHistory(student);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

                float y = 800;

                // Title Banner
                cs.beginText();
                cs.setFont(fontBold, 20);
                cs.newLineAtOffset(50, y);
                cs.showText("SMART ADAPTIVE QUIZ");
                cs.endText();

                y -= 25;
                cs.beginText();
                cs.setFont(fontBold, 14);
                cs.newLineAtOffset(50, y);
                cs.showText("STUDENT PERFORMANCE & PROGRESS REPORT");
                cs.endText();

                y -= 15;
                cs.setLineWidth(1.5f);
                cs.moveTo(50, y);
                cs.lineTo(550, y);
                cs.stroke();

                // Student Details Section
                y -= 30;
                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.newLineAtOffset(50, y);
                cs.showText("STUDENT PROFILE");
                cs.endText();

                y -= 18;
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(50, y);
                cs.showText("Student Name: " + (student.getFullName() != null ? student.getFullName() : student.getUsername()));
                cs.endText();

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(320, y);
                cs.showText("Username: " + student.getUsername());
                cs.endText();

                y -= 15;
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(50, y);
                cs.showText("User ID: " + student.getId());
                cs.endText();

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(320, y);
                cs.showText("Report Generated: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                cs.endText();

                // Key Performance Metrics
                y -= 30;
                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.newLineAtOffset(50, y);
                cs.showText("SUMMARY PERFORMANCE METRICS");
                cs.endText();

                y -= 20;
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(50, y);
                cs.showText("Total Questions Attempted: " + stats.getTotalQuestionsAttempted());
                cs.endText();

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(320, y);
                cs.showText("Overall Accuracy: " + String.format("%.1f%%", stats.getOverallAccuracy()));
                cs.endText();

                y -= 15;
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(50, y);
                cs.showText("Total Correct Answers: " + stats.getTotalCorrect());
                cs.endText();

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(320, y);
                cs.showText("Total Incorrect Answers: " + stats.getTotalIncorrect());
                cs.endText();

                y -= 15;
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(50, y);
                cs.showText("Average Response Time: " + String.format("%.1f seconds", stats.getAverageResponseTimeSec()));
                cs.endText();

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(320, y);
                cs.showText("Word Search Puzzles Completed: " + stats.getTotalPuzzlesCompleted());
                cs.endText();

                y -= 15;
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(50, y);
                cs.showText("Strongest Topic: " + (stats.getStrongestTopic() != null ? stats.getStrongestTopic() : "N/A"));
                cs.endText();

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(320, y);
                cs.showText("Weakest Topic: " + (stats.getWeakestTopic() != null ? stats.getWeakestTopic() : "N/A"));
                cs.endText();

                // Topic Performance Breakdown
                y -= 30;
                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.newLineAtOffset(50, y);
                cs.showText("TOPIC-WISE ACCURACY & ADAPTIVE DIFFICULTY");
                cs.endText();

                y -= 18;
                cs.beginText();
                cs.setFont(fontBold, 10);
                cs.newLineAtOffset(50, y);
                cs.showText(String.format("%-32s %-12s %-12s %-12s %-10s", "Topic", "Attempts", "Correct", "Accuracy", "Difficulty"));
                cs.endText();

                y -= 5;
                cs.setLineWidth(0.8f);
                cs.moveTo(50, y);
                cs.lineTo(550, y);
                cs.stroke();

                if (topics != null && !topics.isEmpty()) {
                    for (TopicPerformanceDto t : topics) {
                        y -= 15;
                        if (y < 80) break;
                        String topicName = t.getTopic();
                        if (topicName.length() > 28) topicName = topicName.substring(0, 25) + "...";
                        String diff = t.getCurrentDifficulty() != null ? t.getCurrentDifficulty() : "EASY";

                        cs.beginText();
                        cs.setFont(fontRegular, 9);
                        cs.newLineAtOffset(50, y);
                        cs.showText(String.format("%-32s %-12d %-12d %-12.1f%% %-10s",
                                topicName, t.getTotalQuestions(), t.getCorrectQuestions(), t.getAccuracyPercentage(), diff));
                        cs.endText();
                    }
                } else {
                    y -= 15;
                    cs.beginText();
                    cs.setFont(fontOblique, 9);
                    cs.newLineAtOffset(50, y);
                    cs.showText("No topic attempts recorded yet.");
                    cs.endText();
                }

                // Recent Quiz History
                y -= 25;
                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.newLineAtOffset(50, y);
                cs.showText("RECENT QUIZ ATTEMPTS (LAST 5)");
                cs.endText();

                y -= 15;
                if (history != null && !history.isEmpty()) {
                    int count = 0;
                    for (QuizAttempt att : history) {
                        if (++count > 5 || y < 60) break;
                        y -= 14;
                        String qText = att.getQuestionText() != null ? att.getQuestionText() : "";
                        if (qText.length() > 50) qText = qText.substring(0, 47) + "...";
                        String result = att.isCorrect() ? "CORRECT" : "INCORRECT";

                        cs.beginText();
                        cs.setFont(fontRegular, 8.5f);
                        cs.newLineAtOffset(50, y);
                        cs.showText(String.format("[%s] %s - %s (%ds)",
                                result, att.getTopic(), qText, att.getResponseTimeSec()));
                        cs.endText();
                    }
                }

                // Footer
                cs.setLineWidth(0.5f);
                cs.moveTo(50, 45);
                cs.lineTo(550, 45);
                cs.stroke();

                cs.beginText();
                cs.setFont(fontOblique, 8);
                cs.newLineAtOffset(50, 32);
                cs.showText("Smart Adaptive Quiz Platform - Confidential Academic Record");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}
