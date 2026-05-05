package com.quizapp.controller;

import com.quizapp.dto.AnswerRequest;
import com.quizapp.dto.AnswerStatsResponse;
import com.quizapp.dto.LeaderboardResponse;
import com.quizapp.service.AnswerService;
import com.quizapp.service.LeaderboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
@Slf4j
public class AnswerController {
    
    private final AnswerService answerService;
    private final LeaderboardService leaderboardService;
    
    @PostMapping
    public ResponseEntity<Void> submitAnswer(@Valid @RequestBody AnswerRequest request) {
        log.info("Submitting answer for student: {}", request.getStudentId());
        answerService.submitAnswer(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @GetMapping("/stats/question/{questionId}")
    public ResponseEntity<AnswerStatsResponse> getQuestionStats(@PathVariable Long questionId) {
        log.info("Fetching stats for question: {}", questionId);
        AnswerStatsResponse stats = answerService.getQuestionStats(questionId);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/stats/quiz/{quizId}/session/{sessionId}")
    public ResponseEntity<List<AnswerStatsResponse>> getAllQuestionStats(
            @PathVariable Long quizId,
            @PathVariable Long sessionId) {
        log.info("Fetching all question stats for quiz: {} and session: {}", quizId, sessionId);
        List<AnswerStatsResponse> stats = answerService.getAllQuestionStats(quizId, sessionId);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/leaderboard/{sessionId}")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(@PathVariable Long sessionId) {
        log.info("Fetching leaderboard for session: {}", sessionId);
        LeaderboardResponse leaderboard = leaderboardService.getLeaderboard(sessionId);
        return ResponseEntity.ok(leaderboard);
    }
}
