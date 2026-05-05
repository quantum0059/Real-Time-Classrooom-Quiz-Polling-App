package com.quizapp.service;

import com.quizapp.dto.QuestionRequest;
import com.quizapp.dto.QuizRequest;
import com.quizapp.exception.ErrorCode;
import com.quizapp.exception.QuizAppException;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {
    
    private final QuizRepository quizRepository;
    private final QuestionService questionService;
    
    @Transactional
    public Quiz createQuiz(QuizRequest request) {
        log.info("Creating quiz with title: {}", request.getTitle());
        
        Quiz quiz = Quiz.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .questions(new ArrayList<>())
            .build();
        
        Quiz savedQuiz = quizRepository.save(quiz);
        
        // Save questions
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            int order = 0;
            for (QuestionRequest questionRequest : request.getQuestions()) {
                    if (questionRequest.getCorrectAnswer() == null
                    || questionRequest.getCorrectAnswer() < 0
                    || questionRequest.getCorrectAnswer() >= questionRequest.getOptions().size()) {
                    throw new QuizAppException(
                        "Correct answer index is invalid for question: " + questionRequest.getText(),
                        ErrorCode.INVALID_OPTION,
                        400
                    );
                }
                Question question = Question.builder()
                    .quiz(savedQuiz)
                    .text(questionRequest.getText())
                    .options(questionRequest.getOptions())
                    .correctAnswer(questionRequest.getCorrectAnswer())
                    .questionOrder(questionRequest.getQuestionOrder() != null ? questionRequest.getQuestionOrder() : order)
                    .build();
                
                Question savedQuestion = questionService.saveQuestion(question);
                savedQuiz.getQuestions().add(savedQuestion);
                order++;
            }
        }
        
        log.info("Quiz created successfully with id: {}", savedQuiz.getId());
        return savedQuiz;
    }
    
    @Transactional(readOnly = true)
    public Quiz getQuizById(Long id) {
        log.info("Fetching quiz with id: {}", id);
        return quizRepository.findById(id)
            .orElseThrow(() -> new QuizAppException(
                "Quiz not found with id: " + id,
                ErrorCode.QUIZ_NOT_FOUND,
                404
            ));
    }
    
    @Transactional(readOnly = true)
    public List<Quiz> getAllQuizzes() {
        log.info("Fetching all quizzes");
        return quizRepository.findAll();
    }
    
    @Transactional
    public void deleteQuiz(Long id) {
        log.info("Deleting quiz: {}", id);
        quizRepository.deleteById(id);
    }
}
