package com.quizapp.service;

import com.quizapp.exception.ErrorCode;
import com.quizapp.exception.QuizAppException;
import com.quizapp.model.Question;
import com.quizapp.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {
    
    private final QuestionRepository questionRepository;
    
    @Transactional
    public Question saveQuestion(Question question) {
        log.info("Saving question: {}", question.getText());
        return questionRepository.save(question);
    }
    
    @Transactional(readOnly = true)
    public Question getQuestionById(Long id) {
        log.info("Fetching question with id: {}", id);
        return questionRepository.findById(id)
            .orElseThrow(() -> new QuizAppException(
                "Question not found with id: " + id,
                ErrorCode.QUESTION_NOT_FOUND,
                404
            ));
    }
    
    @Transactional(readOnly = true)
    public List<Question> getQuestionsByQuizId(Long quizId) {
        log.info("Fetching all questions for quiz: {}", quizId);
        return questionRepository.findByQuizIdOrderByQuestionOrder(quizId);
    }
    
    @Transactional
    public void deleteQuestion(Long id) {
        log.info("Deleting question: {}", id);
        questionRepository.deleteById(id);
    }
}
