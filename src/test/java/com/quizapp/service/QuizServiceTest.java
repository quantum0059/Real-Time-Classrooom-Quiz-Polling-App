package com.quizapp.service;

import com.quizapp.dto.QuizRequest;
import com.quizapp.dto.QuestionRequest;
import com.quizapp.model.Quiz;
import com.quizapp.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class QuizServiceTest {
    
    @Autowired
    private QuizService quizService;
    
    @Autowired
    private QuizRepository quizRepository;
    
    private QuizRequest quizRequest;
    
    @BeforeEach
    public void setup() {
        quizRepository.deleteAll();
        
        QuestionRequest question = QuestionRequest.builder()
            .text("Test Question?")
            .options(Arrays.asList("A", "B", "C", "D"))
            .correctAnswer(0)
            .questionOrder(0)
            .build();
        
        quizRequest = QuizRequest.builder()
            .title("Test Quiz")
            .description("Test Description")
            .questions(Arrays.asList(question))
            .build();
    }
    
    @Test
    public void testCreateQuiz() {
        Quiz quiz = quizService.createQuiz(quizRequest);
        
        assertNotNull(quiz.getId());
        assertEquals("Test Quiz", quiz.getTitle());
        assertEquals(1, quiz.getQuestions().size());
    }
    
    @Test
    public void testGetQuizById() {
        Quiz createdQuiz = quizService.createQuiz(quizRequest);
        Quiz retrievedQuiz = quizService.getQuizById(createdQuiz.getId());
        
        assertNotNull(retrievedQuiz);
        assertEquals(createdQuiz.getId(), retrievedQuiz.getId());
    }
    
    @Test
    public void testGetAllQuizzes() {
        quizService.createQuiz(quizRequest);
        
        var quizzes = quizService.getAllQuizzes();
        assertTrue(quizzes.size() > 0);
    }
}
