package com.quizapp.service;

import com.quizapp.dto.AnswerRequest;
import com.quizapp.dto.QuizRequest;
import com.quizapp.dto.QuestionRequest;
import com.quizapp.exception.QuizAppException;
import com.quizapp.model.Quiz;
import com.quizapp.model.Session;
import com.quizapp.model.Student;
import com.quizapp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AnswerServiceTest {
    
    @Autowired
    private AnswerService answerService;
    
    @Autowired
    private QuizService quizService;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private StudentService studentService;
    
    @Autowired
    private QuizRepository quizRepository;
    
    @Autowired
    private SessionRepository sessionRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private AnswerRepository answerRepository;
    
    private Long quizId;
    private Long sessionId;
    private String sessionCode;
    private Long studentId;
    private Long questionId;
    
    @BeforeEach
    public void setup() {
        // Clean up
        answerRepository.deleteAll();
        studentRepository.deleteAll();
        sessionRepository.deleteAll();
        quizRepository.deleteAll();
        
        // Create quiz
        QuestionRequest question = QuestionRequest.builder()
            .text("Test Question?")
            .options(Arrays.asList("A", "B", "C", "D"))
            .correctAnswer(1)
            .questionOrder(0)
            .build();
        
        QuizRequest quizRequest = QuizRequest.builder()
            .title("Test Quiz")
            .questions(Arrays.asList(question))
            .build();
        
        Quiz quiz = quizService.createQuiz(quizRequest);
        quizId = quiz.getId();
        questionId = quiz.getQuestions().get(0).getId();
        
        // Create session
        Session session = sessionService.startSession(quiz);
        sessionId = session.getId();
        sessionCode = session.getCode();
        
        // Create student
        Student student = Student.builder()
            .name("Test Student")
            .session(session)
            .build();
        student = studentService.saveStudent(student);
        studentId = student.getId();
    }
    
    @Test
    public void testSubmitAnswer() {
        AnswerRequest request = AnswerRequest.builder()
            .studentId(studentId)
            .questionId(questionId)
            .selectedOption(1)
            .sessionCode(sessionCode)
            .build();
        
        assertDoesNotThrow(() -> answerService.submitAnswer(request));
    }
    
    @Test
    public void testDuplicateAnswerThrowsException() {
        AnswerRequest request = AnswerRequest.builder()
            .studentId(studentId)
            .questionId(questionId)
            .selectedOption(1)
            .sessionCode(sessionCode)
            .build();
        
        // First answer should succeed
        assertDoesNotThrow(() -> answerService.submitAnswer(request));
        
        // Second answer should fail
        assertThrows(QuizAppException.class, () -> answerService.submitAnswer(request));
    }
    
    @Test
    public void testGetQuestionStats() {
        AnswerRequest request = AnswerRequest.builder()
            .studentId(studentId)
            .questionId(questionId)
            .selectedOption(0)
            .sessionCode(sessionCode)
            .build();
        
        answerService.submitAnswer(request);
        var stats = answerService.getQuestionStats(questionId);
        
        assertNotNull(stats);
        assertEquals(questionId, stats.getQuestionId());
        assertEquals(1, stats.getTotalResponses());
        assertTrue(stats.getAnswerCounts().containsKey(0));
    }
}
