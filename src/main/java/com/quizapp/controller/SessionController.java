package com.quizapp.controller;

import com.quizapp.dto.JoinSessionRequest;
import com.quizapp.dto.SessionResponse;
import com.quizapp.dto.StudentResponse;
import com.quizapp.model.Question;
import com.quizapp.model.Quiz;
import com.quizapp.model.Session;
import com.quizapp.model.Student;
import com.quizapp.service.QuizService;
import com.quizapp.service.SessionService;
import com.quizapp.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {
    
    private final SessionService sessionService;
    private final QuizService quizService;
    private final StudentService studentService;
    
    @PostMapping("/start/{quizId}")
    public ResponseEntity<SessionResponse> startSession(@PathVariable Long quizId) {
        log.info("Starting new session for quiz: {}", quizId);
        
        Quiz quiz = quizService.getQuizById(quizId);
        Session session = sessionService.startSession(quiz);
        
        SessionResponse response = mapToSessionResponse(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{code}")
    public ResponseEntity<SessionResponse> getSessionByCode(@PathVariable String code) {
        log.info("Fetching session with code: {}", code);
        Session session = sessionService.getSessionByCodeWithDetails(code);
        SessionResponse response = mapToSessionResponse(session);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/join/{code}")
    public ResponseEntity<StudentResponse> joinSession(
            @PathVariable String code,
            @Valid @RequestBody JoinSessionRequest request) {
        log.info("Student joining session: {}", code);
        
        Session session = sessionService.getSessionByCode(code);
        sessionService.validateSessionIsActive(code);
        
        Student student = Student.builder()
            .name(request.getName())
            .session(session)
            .build();
        
        Student savedStudent = studentService.saveStudent(student);
        StudentResponse response = mapToStudentResponse(savedStudent);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{code}/students")
    public ResponseEntity<List<StudentResponse>> getSessionStudents(@PathVariable String code) {
        log.info("Fetching students for session: {}", code);
        Session session = sessionService.getSessionByCodeWithDetails(code);
        
        List<StudentResponse> responses = session.getStudents().stream()
            .map(this::mapToStudentResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{sessionId}/end")
    public ResponseEntity<SessionResponse> endSession(@PathVariable Long sessionId) {
        log.info("Ending session: {}", sessionId);
        Session session = sessionService.endSession(sessionId);
        SessionResponse response = mapToSessionResponse(session);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{sessionId}/question/{questionIndex}")
    public ResponseEntity<SessionResponse> updateCurrentQuestion(
            @PathVariable Long sessionId,
            @PathVariable Integer questionIndex) {
        log.info("Updating current question for session: {}", sessionId);
        Session session = sessionService.updateCurrentQuestion(sessionId, questionIndex);
        SessionResponse response = mapToSessionResponse(session);
        return ResponseEntity.ok(response);
    }
    
    private SessionResponse mapToSessionResponse(Session session) {
        List<Question> questions = session.getQuiz() != null ? 
            session.getQuiz().getQuestions() : null;
        
        List<com.quizapp.dto.QuestionResponse> questionResponses = questions != null ?
            questions.stream()
                .map(q -> com.quizapp.dto.QuestionResponse.builder()
                    .id(q.getId())
                    .text(q.getText())
                    .options(q.getOptions())
                    .build())
                .collect(Collectors.toList())
            : List.of();
        
        return SessionResponse.builder()
            .id(session.getId())
            .code(session.getCode())
            .status(session.getStatus().name())
            .quizId(session.getQuiz() != null ? session.getQuiz().getId() : null)
            .createdAt(session.getCreatedAt())
            .endedAt(session.getEndedAt())
            .currentQuestionIndex(session.getCurrentQuestionIndex())
            .participantCount(session.getStudents().size())
            .questions(questionResponses)
            .build();
    }
    
    private StudentResponse mapToStudentResponse(Student student) {
        return StudentResponse.builder()
            .id(student.getId())
            .studentId(student.getStudentId())
            .name(student.getName())
            .joinedAt(student.getJoinedAt())
            .correctAnswers(student.getCorrectAnswers())
            .build();
    }
}
