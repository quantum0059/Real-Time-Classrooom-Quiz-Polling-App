package com.quizapp.websocket;

import com.quizapp.dto.AnswerRequest;
import com.quizapp.dto.AnswerStatsResponse;
import com.quizapp.dto.LeaderboardResponse;
import com.quizapp.model.Session;
import com.quizapp.service.AnswerService;
import com.quizapp.service.LeaderboardService;
import com.quizapp.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class QuizWebSocketHandler {
    
    private final AnswerService answerService;
    private final LeaderboardService leaderboardService;
    private final SessionService sessionService;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Handle student answer submission via WebSocket
     * Message mapping: /app/answer
     * Publishes updated stats and leaderboard to session topics
     */
    @MessageMapping("/answer")
    public void handleStudentAnswer(@Payload AnswerRequest request) {
        log.info("Received WebSocket answer from student: {}", request.getStudentId());

        answerService.submitAnswer(request);
        AnswerStatsResponse stats = answerService.getQuestionStats(request.getQuestionId());

        Session session = sessionService.getSessionByCode(request.getSessionCode());
        LeaderboardResponse leaderboard = leaderboardService.getLeaderboard(session.getId());

        String statsDestination = "/topic/session/" + session.getId();
        String leaderboardDestination = "/topic/leaderboard/" + session.getId();

        messagingTemplate.convertAndSend(statsDestination, stats);
        messagingTemplate.convertAndSend(leaderboardDestination, leaderboard);

        log.info("Broadcasted updated session stats and leaderboard for session: {}", session.getId());
    }
    
    /**
     * Handle explicit leaderboard request via WebSocket
     * Message mapping: /app/leaderboard/{sessionId}
     * Publishes to: /topic/leaderboard/{sessionId}
     */
    @MessageMapping("/leaderboard/{sessionId}")
    public LeaderboardResponse getLeaderboard(@DestinationVariable Long sessionId) {
        log.info("Leaderboard requested for session: {}", sessionId);
        return leaderboardService.getLeaderboard(sessionId);
    }
}
