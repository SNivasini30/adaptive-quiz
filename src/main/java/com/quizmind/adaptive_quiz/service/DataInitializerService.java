package com.quizmind.adaptive_quiz.service;

import com.quizmind.adaptive_quiz.model.*;
import com.quizmind.adaptive_quiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class DataInitializerService implements CommandLineRunner {

    private final QuestionRepository questionRepository;
    private final PdfDocumentRepository pdfDocumentRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final StudentTopicProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public DataInitializerService(QuestionRepository questionRepository,
                                  PdfDocumentRepository pdfDocumentRepository,
                                  QuizAttemptRepository quizAttemptRepository,
                                  StudentTopicProgressRepository progressRepository,
                                  UserRepository userRepository,
                                  UserProfileRepository userProfileRepository,
                                  org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.questionRepository = questionRepository;
        this.pdfDocumentRepository = pdfDocumentRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE questions ALTER COLUMN approval_status VARCHAR(32)");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE questions ALTER COLUMN question_type VARCHAR(32)");
        } catch (Exception ignored) {}
        // 1. Seed Teacher Account
        User teacher = userRepository.findByUsernameIgnoreCase("teacher").orElse(null);
        if (teacher == null) {
            teacher = new User("teacher", passwordEncoder.encode("teacher123"), "Professor Davis", "teacher@smartquiz.edu", UserRole.TEACHER);
            teacher.setCreatedAt(LocalDateTime.now().minusDays(30));
            teacher.setLastLoginAt(LocalDateTime.now());
            teacher = userRepository.save(teacher);

            UserProfile teacherProfile = new UserProfile(teacher);
            teacherProfile.setBio("Faculty Department of Computer Science & Engineering");
            teacherProfile.setInstitution("Metropolitan University");
            userProfileRepository.save(teacherProfile);
        }

        // 2. Seed Default Student Account
        User student = userRepository.findByUsernameIgnoreCase("student").orElse(null);
        if (student == null) {
            student = new User("student", passwordEncoder.encode("student123"), "Student User", "student@smartquiz.edu", UserRole.STUDENT);
            student.setCreatedAt(LocalDateTime.now().minusDays(14));
            student.setLastLoginAt(LocalDateTime.now());
            student = userRepository.save(student);

            UserProfile studentProfile = new UserProfile(student);
            studentProfile.setBio("Enthusiastic adaptive learner studying computer science.");
            studentProfile.setInstitution("Metropolitan University");
            userProfileRepository.save(studentProfile);
        }

        // Backfill legacy attempts or progress if user is null
        List<QuizAttempt> existingAttempts = quizAttemptRepository.findAll();
        for (QuizAttempt qa : existingAttempts) {
            if (qa.getUser() == null) {
                qa.setUser(student);
                quizAttemptRepository.save(qa);
            }
        }

        List<StudentTopicProgress> existingProgress = progressRepository.findAll();
        for (StudentTopicProgress stp : existingProgress) {
            if (stp.getUser() == null) {
                stp.setUser(student);
                progressRepository.save(stp);
            }
        }

        if (questionRepository.count() > 0) {
            return;
        }

        // 3. Initial Sample Educational Document
        PdfDocument doc = new PdfDocument(
                "Computer_Science_Core_Concepts.pdf",
                458200L,
                14,
                "An operating system is system software that manages computer hardware and software resources. Encapsulation is the technique of making the fields in a class private and providing access via public methods. Virtual memory enables execution of processes that may not be completely in memory...",
                "PROCESSED"
        );
        doc.setUploadedBy(teacher);
        doc = pdfDocumentRepository.save(doc);

        // 4. Approved Questions for Quiz & Word Search Puzzle
        GeneratedQuestion q1 = new GeneratedQuestion(
                "What is the pillar of OOP that bundles data and methods while restricting direct access to internal fields?",
                "Encapsulation",
                Arrays.asList("Encapsulation", "Polymorphism", "Inheritance", "Abstraction"),
                "Object-Oriented Programming",
                Difficulty.EASY,
                QuestionType.MCQ,
                "Encapsulation is the technique of making the fields in a class private and providing access via public methods.",
                "Encapsulation hides internal data implementation details and protects state consistency.",
                ApprovalStatus.APPROVED
        );
        q1.setDocument(doc);

        GeneratedQuestion q2 = new GeneratedQuestion(
                "Which concept allows a subclass to provide a specific implementation of a method of its superclass?",
                "Method Overriding",
                Arrays.asList("Method Overriding", "Method Overloading", "Method Hiding", "Encapsulation"),
                "Object-Oriented Programming",
                Difficulty.MEDIUM,
                QuestionType.MCQ,
                "Method overriding allows a subclass to customize or replace parent method implementations.",
                "Method overriding is resolved at runtime dynamically via virtual method dispatch.",
                ApprovalStatus.APPROVED
        );
        q2.setDocument(doc);

        GeneratedQuestion q3 = new GeneratedQuestion(
                "Which OOP principle allows entities to take on multiple forms depending on runtime context?",
                "Polymorphism",
                Arrays.asList("Polymorphism", "Recursion", "Encapsulation", "Compilation"),
                "Object-Oriented Programming",
                Difficulty.MEDIUM,
                QuestionType.MCQ,
                "Polymorphism allows objects of different classes to be treated through a common superclass reference.",
                "Polymorphism enhances extensibility and code decoupling.",
                ApprovalStatus.APPROVED
        );
        q3.setDocument(doc);

        GeneratedQuestion q4 = new GeneratedQuestion(
                "________ is system software that manages computer hardware and software resources.",
                "Operating System",
                Arrays.asList("Operating System", "Compiler", "Database Engine", "Hypervisor"),
                "Operating Systems",
                Difficulty.EASY,
                QuestionType.FILL_IN_BLANK,
                "An operating system is system software that manages computer hardware and software resources.",
                "The OS acts as the mediator between application software and underlying hardware devices.",
                ApprovalStatus.APPROVED
        );
        q4.setDocument(doc);

        GeneratedQuestion q5 = new GeneratedQuestion(
                "What is the condition where a set of processes are blocked because each holds a resource while waiting for another?",
                "Deadlock",
                Arrays.asList("Deadlock", "Paging", "Thrashing", "Starvation"),
                "Operating Systems",
                Difficulty.HARD,
                QuestionType.MCQ,
                "Deadlock is a state where concurrent processes wait mutually and endlessly for resource releases.",
                "The 4 Coffman conditions for deadlock: Mutual Exclusion, Hold & Wait, No Preemption, and Circular Wait.",
                ApprovalStatus.APPROVED
        );
        q5.setDocument(doc);

        GeneratedQuestion q6 = new GeneratedQuestion(
                "________ is an efficient search algorithm that repeatedly divides a sorted search interval in half.",
                "Binary Search",
                Arrays.asList("Binary Search", "Linear Search", "Bubble Sort", "Depth First Search"),
                "Data Structures",
                Difficulty.EASY,
                QuestionType.FILL_IN_BLANK,
                "A binary search is an efficient search algorithm that repeatedly divides a sorted search interval in half.",
                "Binary search achieves logarithmic time complexity O(log n) on sorted arrays.",
                ApprovalStatus.APPROVED
        );
        q6.setDocument(doc);

        GeneratedQuestion q7 = new GeneratedQuestion(
                "What is a programming technique where a method calls itself directly or indirectly until a base condition is met?",
                "Recursion",
                Arrays.asList("Recursion", "Iteration", "Inheritance", "Paging"),
                "Data Structures",
                Difficulty.MEDIUM,
                QuestionType.MCQ,
                "Recursion is a method where the solution depends on solutions to smaller instances of the same problem.",
                "Recursion relies on the call stack memory and requires a well-defined base case to avoid stack overflow.",
                ApprovalStatus.APPROVED
        );
        q7.setDocument(doc);

        GeneratedQuestion q8 = new GeneratedQuestion(
                "Which memory management capability provides an idealized abstraction of storage resources allowing processes to exceed physical RAM?",
                "Virtual Memory",
                Arrays.asList("Virtual Memory", "Deadlock", "Cache Invalidation", "Buffer Overflow"),
                "Operating Systems",
                Difficulty.MEDIUM,
                QuestionType.MCQ,
                "Virtual memory maps virtual addresses used by programs to physical addresses in computer memory.",
                "Virtual memory uses paging and swapping to extend usable address space seamlessly.",
                ApprovalStatus.APPROVED
        );
        q8.setDocument(doc);

        // 5. Questions in GENERATED status (ready for Teacher Review & Approval tab!)
        GeneratedQuestion q9 = new GeneratedQuestion(
                "________ is a linear data structure that follows the First-In-First-Out FIFO principle.",
                "Queue",
                Arrays.asList("Queue", "Stack", "Binary Search", "Linked List"),
                "Data Structures",
                Difficulty.EASY,
                QuestionType.FILL_IN_BLANK,
                "A queue is a linear collection designed for holding elements prior to processing in FIFO order.",
                "Queues support enqueue at the rear and dequeue from the front.",
                ApprovalStatus.GENERATED
        );
        q9.setDocument(doc);

        GeneratedQuestion q10 = new GeneratedQuestion(
                "Which component of an operating system manages thread scheduling and processor time slicing?",
                "Process Scheduler",
                Arrays.asList("Process Scheduler", "File System", "Virtual Memory", "Device Driver"),
                "Operating Systems",
                Difficulty.MEDIUM,
                QuestionType.MCQ,
                "The process scheduler determines which process runs when there are multiple runnable processes.",
                "Preemptive and cooperative scheduling are the primary scheduling philosophies.",
                ApprovalStatus.GENERATED
        );
        q10.setDocument(doc);

        GeneratedQuestion q11 = new GeneratedQuestion(
                "________ is the mechanism wherein a derived class acquires attributes and behaviors from a base class.",
                "Inheritance",
                Arrays.asList("Inheritance", "Encapsulation", "Polymorphism", "Abstraction"),
                "Object-Oriented Programming",
                Difficulty.EASY,
                QuestionType.FILL_IN_BLANK,
                "Inheritance promotes software reuse by establishing an 'is-a' relationship between classes.",
                "Java supports single inheritance for classes and multiple inheritance through interfaces.",
                ApprovalStatus.GENERATED
        );
        q11.setDocument(doc);

        questionRepository.saveAll(Arrays.asList(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, q11));
        doc.setGeneratedQuestionCount(11);
        pdfDocumentRepository.save(doc);

        // 6. Initial student progress per topic
        StudentTopicProgress p1 = new StudentTopicProgress("student", "Object-Oriented Programming");
        p1.setUser(student);
        p1.setCurrentDifficulty(Difficulty.MEDIUM);
        p1.setTotalAttempts(15);
        p1.setCorrectAttempts(12);
        p1.setConsecutiveCorrect(2);

        StudentTopicProgress p2 = new StudentTopicProgress("student", "Operating Systems");
        p2.setUser(student);
        p2.setCurrentDifficulty(Difficulty.EASY);
        p2.setTotalAttempts(12);
        p2.setCorrectAttempts(6);
        p2.setConsecutiveIncorrect(1);

        StudentTopicProgress p3 = new StudentTopicProgress("student", "Data Structures");
        p3.setUser(student);
        p3.setCurrentDifficulty(Difficulty.MEDIUM);
        p3.setTotalAttempts(10);
        p3.setCorrectAttempts(8);
        p3.setConsecutiveCorrect(1);

        progressRepository.saveAll(Arrays.asList(p1, p2, p3));

        // 7. Seed initial realistic QuizAttempts for Dashboard analytics & Mistake Analyzer
        quizAttemptRepository.save(new QuizAttempt(student, q1, "Encapsulation", true, "Object-Oriented Programming", Difficulty.EASY, 8, Confidence.HIGH, "init-session-1"));
        quizAttemptRepository.save(new QuizAttempt(student, q2, "Method Overriding", true, "Object-Oriented Programming", Difficulty.MEDIUM, 14, Confidence.MEDIUM, "init-session-1"));
        quizAttemptRepository.save(new QuizAttempt(student, q3, "Recursion", false, "Object-Oriented Programming", Difficulty.MEDIUM, 22, Confidence.HIGH, "init-session-1")); // mistake
        quizAttemptRepository.save(new QuizAttempt(student, q4, "Operating System", true, "Operating Systems", Difficulty.EASY, 6, Confidence.HIGH, "init-session-2"));
        quizAttemptRepository.save(new QuizAttempt(student, q5, "Thrashing", false, "Operating Systems", Difficulty.HARD, 35, Confidence.MEDIUM, "init-session-2")); // mistake
        quizAttemptRepository.save(new QuizAttempt(student, q8, "Paging", false, "Operating Systems", Difficulty.MEDIUM, 19, Confidence.HIGH, "init-session-2")); // mistake
        quizAttemptRepository.save(new QuizAttempt(student, q6, "Binary Search", true, "Data Structures", Difficulty.EASY, 9, Confidence.HIGH, "init-session-3"));
        quizAttemptRepository.save(new QuizAttempt(student, q7, "Recursion", true, "Data Structures", Difficulty.MEDIUM, 12, Confidence.MEDIUM, "init-session-3"));
    }
}
