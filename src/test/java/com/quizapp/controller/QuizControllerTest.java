package com.quizapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizapp.dto.QuizRequest;
import com.quizapp.dto.QuestionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class QuizControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private QuizRequest quizRequest;
    
    @BeforeEach
    public void setup() {
        QuestionRequest question1 = QuestionRequest.builder()
            .text("What is 2 + 2?")
            .options(Arrays.asList("3", "4", "5", "6"))
            .correctAnswer(1)
            .questionOrder(0)
            .build();
        
        QuestionRequest question2 = QuestionRequest.builder()
            .text("What is the capital of France?")
            .options(Arrays.asList("London", "Paris", "Berlin", "Madrid"))
            .correctAnswer(1)
            .questionOrder(1)
            .build();
        
        quizRequest = QuizRequest.builder()
            .title("Math Quiz")
            .description("Test your math skills")
            .questions(Arrays.asList(question1, question2))
            .build();
    }
    
    @Test
    public void testCreateQuiz() throws Exception {
        mockMvc.perform(post("/api/quizzes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(quizRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("Math Quiz"))
            .andExpect(jsonPath("$.questionCount").value(2));
    }
    
    @Test
    public void testGetAllQuizzes() throws Exception {
        // First create a quiz
        mockMvc.perform(post("/api/quizzes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(quizRequest)))
            .andExpect(status().isCreated());
        
        // Then get all quizzes
        mockMvc.perform(get("/api/quizzes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}
