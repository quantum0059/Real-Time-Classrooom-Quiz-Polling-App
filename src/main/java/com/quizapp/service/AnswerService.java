package com.quizapp.service;

import com.quizapp.dto.AnswerRequest;
import com.quizapp.dto.AnswerStatsResponse;
import com.quizapp.exception.ErrorCode;
import com.quizapp.exception.QuizAppException;
import com.quizapp.model.Answer;
import com.quizapp.model.Question;
import com.quizapp.model.Session;
import com.quizapp.model.Student;
import com.quizapp.repository.AnswerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerService {
    
    private final AnswerRepository answerRepository;
    private final StudentService studentService;
    private final QuestionService questionService;
    private final SessionService sessionService;
    
    @Transactional
    public Answer submitAnswer(AnswerRequest request) {
        log.info("Submitting answer for student: {}, question: {}", request.getStudentId(), request.getQuestionId());
        
        // Validate session is active and fetch session
        Session session = sessionService.getSessionByCode(request.getSessionCode());
        sessionService.validateSessionIsActive(request.getSessionCode());
        
        // Get student and question
        Student student = studentService.getStudentById(request.getStudentId());
        Question question = questionService.getQuestionById(request.getQuestionId());
        
        // Validate student belongs to the session referenced by the session code
        if (!Objects.equals(student.getSession().getId(), session.getId())) {
            throw new QuizAppException(
                "Student does not belong to the session code provided",
                ErrorCode.INVALID_SESSION_CODE,
                400
            );
        }
        
        // Validate the question belongs to the quiz used by the session
        if (!Objects.equals(question.getQuiz().getId(), session.getQuiz().getId())) {
            throw new QuizAppException(
                "Question does not belong to the active session's quiz",
                ErrorCode.QUESTION_NOT_FOUND,
                400
            );
        }
        
        // Validate option is within range
        if (request.getSelectedOption() < 0 || request.getSelectedOption() >= question.getOptions().size()) {
            throw new QuizAppException(
                "Invalid option index: " + request.getSelectedOption(),
                ErrorCode.INVALID_OPTION,
                400
            );
        }
        
        // Check for duplicate answer
        Optional<Answer> existingAnswer = answerRepository.findByStudentIdAndQuestionId(
            request.getStudentId(),
            request.getQuestionId()
        );
        
        if (existingAnswer.isPresent()) {
            throw new QuizAppException(
                "Student has already answered this question",
                ErrorCode.DUPLICATE_ANSWER,
                409
            );
        }
        
        // Create and save answer
        boolean isCorrect = question.getCorrectAnswer() != null && request.getSelectedOption().equals(question.getCorrectAnswer());
        Answer answer = Answer.builder()
            .student(student)
            .question(question)
            .selectedOption(request.getSelectedOption())
            .isCorrect(isCorrect)
            .build();
        
        Answer savedAnswer = answerRepository.save(answer);
        
        // Update student's correct answer count if answer is correct
        if (isCorrect) {
            student.setCorrectAnswers(student.getCorrectAnswers() + 1);
            studentService.updateStudent(student);
        }
        
        log.info("Answer submitted successfully: {}", savedAnswer.getId());
        return savedAnswer;
    }
    
    public AnswerStatsResponse getQuestionStats(Long questionId) {
        log.info("Getting stats for question: {}", questionId);
        
        Question question = questionService.getQuestionById(questionId);
        List<Answer> answers = answerRepository.findByQuestionId(questionId);
        
        Map<Integer, Integer> answerCounts = new HashMap<>();
        Map<String, Double> answerPercentages = new HashMap<>();
        
        // Initialize counts for all options
        for (int i = 0; i < question.getOptions().size(); i++) {
            answerCounts.put(i, 0);
        }
        
        // Count answers
        for (Answer answer : answers) {
            answerCounts.merge(answer.getSelectedOption(), 1, Integer::sum);
        }
        
        // Calculate percentages
        int totalResponses = answers.size();
        for (Map.Entry<Integer, Integer> entry : answerCounts.entrySet()) {
            double percentage = totalResponses > 0 ? (entry.getValue() * 100.0) / totalResponses : 0;
            answerPercentages.put("Option " + (char)('A' + entry.getKey()), percentage);
        }
        
        long correctCount = answers.stream()
            .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
            .count();
        
        return AnswerStatsResponse.builder()
            .questionId(questionId)
            .questionText(question.getText())
            .totalResponses(totalResponses)
            .answerCounts(answerCounts)
            .answerPercentages(answerPercentages)
            .correctAnswer(question.getCorrectAnswer())
            .correctCount(correctCount)
            .build();
    }
    
    public List<AnswerStatsResponse> getAllQuestionStats(Long quizId, Long sessionId) {
        log.info("Getting stats for all questions in quiz: {} and session: {}", quizId, sessionId);
        
        List<Question> questions = questionService.getQuestionsByQuizId(quizId);
        List<Answer> sessionAnswers = answerRepository.findByQuizAndSession(quizId, sessionId);
        
        return questions.stream()
            .map(question -> buildQuestionStats(question, sessionAnswers))
            .collect(Collectors.toList());
    }
    
    private AnswerStatsResponse buildQuestionStats(Question question, List<Answer> sessionAnswers) {
        List<Answer> answers = sessionAnswers.stream()
            .filter(answer -> Objects.equals(answer.getQuestion().getId(), question.getId()))
            .toList();
        
        Map<Integer, Integer> answerCounts = new HashMap<>();
        for (int i = 0; i < question.getOptions().size(); i++) {
            answerCounts.put(i, 0);
        }
        
        for (Answer answer : answers) {
            answerCounts.merge(answer.getSelectedOption(), 1, Integer::sum);
        }
        
        int totalResponses = answers.size();
        Map<String, Double> answerPercentages = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : answerCounts.entrySet()) {
            double percentage = totalResponses > 0 ? (entry.getValue() * 100.0) / totalResponses : 0;
            answerPercentages.put("Option " + (char)('A' + entry.getKey()), percentage);
        }
        
        long correctCount = answers.stream()
            .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
            .count();
        
        return AnswerStatsResponse.builder()
            .questionId(question.getId())
            .questionText(question.getText())
            .totalResponses(totalResponses)
            .answerCounts(answerCounts)
            .answerPercentages(answerPercentages)
            .correctAnswer(question.getCorrectAnswer())
            .correctCount(correctCount)
            .build();
    }
    
    @Transactional(readOnly = true)
    public int getStudentCorrectAnswerCount(Long studentId, Long quizId) {
        return (int) answerRepository
            .findByQuizAndSession(quizId, studentId)
            .stream()
            .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
            .count();
    }
}
