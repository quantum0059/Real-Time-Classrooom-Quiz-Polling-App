package com.quizapp.controller;

import com.quizapp.dto.QuizRequest;
import com.quizapp.dto.QuizResponse;
import com.quizapp.dto.QuestionResponse;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@Slf4j
public class QuizController {
    
    private final QuizService quizService;
    
    @PostMapping
    public ResponseEntity<QuizResponse> createQuiz(@Valid @RequestBody QuizRequest request) {
        log.info("Creating new quiz: {}", request.getTitle());
        Quiz quiz = quizService.createQuiz(request);
        QuizResponse response = mapToQuizResponse(quiz);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<QuizResponse> getQuizById(@PathVariable Long id) {
        log.info("Fetching quiz: {}", id);
        Quiz quiz = quizService.getQuizById(id);
        QuizResponse response = mapToQuizResponse(quiz);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<QuizResponse>> getAllQuizzes() {
        log.info("Fetching all quizzes");
        List<Quiz> quizzes = quizService.getAllQuizzes();
        List<QuizResponse> responses = quizzes.stream()
            .map(this::mapToQuizResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id) {
        log.info("Deleting quiz: {}", id);
        quizService.deleteQuiz(id);
        return ResponseEntity.noContent().build();
    }
    
    private QuizResponse mapToQuizResponse(Quiz quiz) {
        List<QuestionResponse> questionResponses = quiz.getQuestions().stream()
            .map(question -> QuestionResponse.builder()
                .id(question.getId())
                .text(question.getText())
                .options(question.getOptions())
                .build())
            .collect(Collectors.toList());

        return QuizResponse.builder()
            .id(quiz.getId())
            .title(quiz.getTitle())
            .description(quiz.getDescription())
            .questionCount(quiz.getQuestions().size())
            .questions(questionResponses)
            .build();
    }
}
