package com.quizapp.config;

import com.quizapp.dto.QuestionRequest;
import com.quizapp.dto.QuizRequest;
import com.quizapp.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class SampleDataLoader implements ApplicationRunner {

    private final QuizService quizService;

    @Override
    public void run(ApplicationArguments args) {
        if (!quizService.getAllQuizzes().isEmpty()) {
            return;
        }

        QuizRequest sampleQuiz = QuizRequest.builder()
            .title("Classroom Java Basics Quiz")
            .description("A quick sample quiz for Java and Spring Boot fundamentals.")
            .questions(List.of(
                QuestionRequest.builder()
                    .text("Which keyword is used to create an immutable class in Java?")
                    .options(List.of("final", "static", "volatile", "transient"))
                    .correctAnswer(0)
                    .build(),
                QuestionRequest.builder()
                    .text("Which Spring annotation is used to define a REST controller?")
                    .options(List.of("@Component", "@Service", "@Controller", "@RestController"))
                    .correctAnswer(3)
                    .build(),
                QuestionRequest.builder()
                    .text("What does STOMP provide when used with WebSocket?")
                    .options(List.of("TCP transport", "Message semantics", "Database access", "Static file hosting"))
                    .correctAnswer(1)
                    .build()
            ))
            .build();

        quizService.createQuiz(sampleQuiz);
        log.info("Loaded sample quiz data for development.");
    }
}
