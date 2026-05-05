package com.quizapp.websocket;

import com.quizapp.dto.AnswerRequest;
import com.quizapp.dto.AnswerStatsResponse;
import com.quizapp.dto.LeaderboardResponse;
import com.quizapp.service.AnswerService;
import com.quizapp.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class QuizWebSocketHandler {
    
    private final AnswerService answerService;
    private final LeaderboardService leaderboardService;
    
    /**
     * Handle student answer submission via WebSocket
     * Message mapping: /app/answer
     * Publishes to: /topic/session/{sessionId}
     */
    @MessageMapping("/answer")
    @SendTo("/topic/session/{sessionId}")
    public AnswerStatsResponse handleStudentAnswer(AnswerRequest request) {
        log.info("Received answer from student: {}", request.getStudentId());
        
        try {
            // Submit answer and get updated statistics
            answerService.submitAnswer(request);
            
            // Return updated stats for the question
            AnswerStatsResponse stats = answerService.getQuestionStats(request.getQuestionId());
            log.info("Answer processed successfully");
            return stats;
            
        } catch (Exception e) {
            log.error("Error processing answer: ", e);
            throw e;
        }
    }
    
    /**
     * Handle leaderboard request via WebSocket
     * Message mapping: /app/leaderboard
     * Publishes to: /topic/leaderboard/{sessionId}
     */
    @MessageMapping("/leaderboard/{sessionId}")
    @SendTo("/topic/leaderboard/{sessionId}")
    public LeaderboardResponse getLeaderboard(Long sessionId) {
        log.info("Leaderboard requested for session: {}", sessionId);
        return leaderboardService.getLeaderboard(sessionId);
    }
}
