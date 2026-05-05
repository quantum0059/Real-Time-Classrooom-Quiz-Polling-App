package com.quizapp.service;

import com.quizapp.exception.ErrorCode;
import com.quizapp.exception.QuizAppException;
import com.quizapp.model.Student;
import com.quizapp.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {
    
    private final StudentRepository studentRepository;
    
    @Transactional
    public Student saveStudent(Student student) {
        log.info("Saving student: {}", student.getName());
        return studentRepository.save(student);
    }
    
    @Transactional(readOnly = true)
    public Student getStudentById(Long id) {
        log.info("Fetching student with id: {}", id);
        return studentRepository.findById(id)
            .orElseThrow(() -> new QuizAppException(
                "Student not found with id: " + id,
                ErrorCode.STUDENT_NOT_FOUND,
                404
            ));
    }
    
    @Transactional(readOnly = true)
    public Student getStudentByStudentId(String studentId) {
        log.info("Fetching student with studentId: {}", studentId);
        return studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new QuizAppException(
                "Student not found with studentId: " + studentId,
                ErrorCode.STUDENT_NOT_FOUND,
                404
            ));
    }
    
    @Transactional(readOnly = true)
    public List<Student> getStudentsBySessionIdOrderedByScore(Long sessionId) {
        log.info("Fetching students for session: {}", sessionId);
        return studentRepository.findBySessionIdOrderByCorrectAnswersDesc(sessionId);
    }
    
    @Transactional(readOnly = true)
    public long getParticipantCount(Long sessionId) {
        log.info("Getting participant count for session: {}", sessionId);
        return studentRepository.countBySessionId(sessionId);
    }
    
    @Transactional
    public Student updateStudent(Student student) {
        log.info("Updating student: {}", student.getName());
        return studentRepository.save(student);
    }
}
