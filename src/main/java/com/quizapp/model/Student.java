package com.quizapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String studentId; // Unique identifier (UUID or generated)
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private Session session;
    
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Answer> answers = new ArrayList<>();
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;
    
    @Column(nullable = false)
    private Integer correctAnswers = 0;
    
    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        if (studentId == null) {
            studentId = UUID.randomUUID().toString();
        }
        if (correctAnswers == null) {
            correctAnswers = 0;
        }
    }
}
