package com.quizapp.service;

import com.quizapp.exception.ErrorCode;
import com.quizapp.exception.QuizAppException;
import com.quizapp.model.Session;
import com.quizapp.model.SessionStatus;
import com.quizapp.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {
    
    private final SessionRepository sessionRepository;
    private static final int MAX_RETRIES = 5;
    private static final int CODE_LENGTH = 6;
    
    @Transactional
    public Session startSession(Long quizId) {
        log.info("Starting session for quiz: {}", quizId);
        
        Session session = Session.builder()
            .code(generateUniqueSessionCode())
            .status(SessionStatus.ACTIVE)
            .currentQuestionIndex(0)
            .build();
        
        Session savedSession = sessionRepository.save(session);
        log.info("Session started with code: {}", savedSession.getCode());
        return savedSession;
    }
    
    @Transactional(readOnly = true)
    public Session getSessionByCode(String code) {
        log.info("Fetching session with code: {}", code);
        return sessionRepository.findByCode(code)
            .orElseThrow(() -> new QuizAppException(
                "Session not found with code: " + code,
                ErrorCode.SESSION_NOT_FOUND,
                404
            ));
    }
    
    @Transactional(readOnly = true)
    public Session getSessionById(Long id) {
        log.info("Fetching session with id: {}", id);
        return sessionRepository.findById(id)
            .orElseThrow(() -> new QuizAppException(
                "Session not found with id: " + id,
                ErrorCode.SESSION_NOT_FOUND,
                404
            ));
    }
    
    @Transactional
    public Session endSession(Long sessionId) {
        log.info("Ending session: {}", sessionId);
        Session session = getSessionById(sessionId);
        session.setStatus(SessionStatus.ENDED);
        session.setEndedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }
    
    @Transactional
    public Session updateCurrentQuestion(Long sessionId, Integer questionIndex) {
        log.info("Updating current question for session: {} to index: {}", sessionId, questionIndex);
        Session session = getSessionById(sessionId);
        session.setCurrentQuestionIndex(questionIndex);
        return sessionRepository.save(session);
    }
    
    @Transactional(readOnly = true)
    public void validateSessionIsActive(String code) {
        Session session = getSessionByCode(code);
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new QuizAppException(
                "Session is not active. Current status: " + session.getStatus(),
                ErrorCode.SESSION_INACTIVE,
                400
            );
        }
    }
    
    private String generateUniqueSessionCode() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            String code = RandomStringUtils.randomAlphanumeric(CODE_LENGTH).toUpperCase();
            if (!sessionRepository.findByCode(code).isPresent()) {
                return code;
            }
        }
        throw new QuizAppException(
            "Failed to generate unique session code after " + MAX_RETRIES + " attempts",
            ErrorCode.SESSION_CODE_GENERATION_FAILED,
            500
        );
    }
}
