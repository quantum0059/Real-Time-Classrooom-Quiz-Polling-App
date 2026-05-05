package com.quizapp.service;

import com.quizapp.dto.LeaderboardEntry;
import com.quizapp.dto.LeaderboardResponse;
import com.quizapp.model.Session;
import com.quizapp.model.Student;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {
    
    private final StudentService studentService;
    private final SessionService sessionService;
    
    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(Long sessionId) {
        log.info("Generating leaderboard for session: {}", sessionId);
        
        Session session = sessionService.getSessionById(sessionId);
        List<Student> students = studentService.getStudentsBySessionIdOrderedByScore(sessionId);
        long participantCount = studentService.getParticipantCount(sessionId);
        
        List<LeaderboardEntry> leaderboardEntries = new ArrayList<>();
        int rank = 1;
        
        for (Student student : students) {
            LeaderboardEntry entry = LeaderboardEntry.builder()
                .studentId(student.getId())
                .name(student.getName())
                .correctAnswers(student.getCorrectAnswers())
                .totalAnswered(student.getAnswers().size())
                .score((double) student.getCorrectAnswers())
                .rank(rank++)
                .build();
            
            leaderboardEntries.add(entry);
        }
        
        return LeaderboardResponse.builder()
            .sessionCode(session.getCode())
            .sessionId(sessionId)
            .totalParticipants((int) participantCount)
            .leaderboard(leaderboardEntries)
            .build();
    }
}
