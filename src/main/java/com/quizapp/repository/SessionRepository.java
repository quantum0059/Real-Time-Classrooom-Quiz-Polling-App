package com.quizapp.repository;

import com.quizapp.model.Session;
import com.quizapp.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByCode(String code);
    
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.students WHERE s.code = :code")
    Optional<Session> findByCodeWithStudents(@Param("code") String code);
    
    long countByStatusAndQuizId(SessionStatus status, Long quizId);
}
