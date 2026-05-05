# Real-Time Quiz Application - Architecture & Design Document

## 📐 System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (HTML/JS)                       │
│              WebSocket Client + REST Client                  │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
    ┌─────────┐    ┌──────────┐    ┌──────────┐
    │   WS    │    │ REST API │    │ Browser  │
    │Endpoint │    │ Controller   │ Cache    │
    └────┬────┘    └──────┬───┘    └──────────┘
         │                │
         └────────┬───────┘
                  ▼
        ┌──────────────────────┐
        │  Spring Boot App     │
        │  - Controllers       │
        │  - Services          │
        │  - Repositories      │
        │  - WebSocket Handler │
        │  - Exception Handler │
        └──────────┬───────────┘
                   │
         ┌─────────┴──────────┐
         ▼                    ▼
    ┌─────────────┐      ┌──────────────┐
    │  H2 (Dev)   │      │ PostgreSQL   │
    │   Database  │      │  (Production)│
    └─────────────┘      └──────────────┘
```

## 🏗️ Clean Architecture Layers

### 1. Controller Layer
**Purpose**: Handle HTTP requests and WebSocket messages

**Responsibilities**:
- Parse incoming requests
- Validate input parameters
- Call appropriate services
- Format responses
- Handle routing

**Classes**:
- `QuizController` - Quiz CRUD operations
- `SessionController` - Session management
- `AnswerController` - Answer submission and statistics
- `QuizWebSocketHandler` - WebSocket message handling

### 2. Service Layer
**Purpose**: Implement business logic

**Responsibilities**:
- Validate business rules
- Coordinate operations between repositories
- Handle transactions
- Manage caching
- Throw domain exceptions

**Classes**:
- `QuizService` - Quiz operations
- `QuestionService` - Question management
- `SessionService` - Session lifecycle and code generation
- `StudentService` - Student enrollment
- `AnswerService` - Answer submission and validation
- `LeaderboardService` - Ranking calculations

### 3. Repository Layer
**Purpose**: Data access abstraction

**Responsibilities**:
- Execute database queries
- Abstract database operations
- Provide finder methods
- Handle transactions at database level

**Interfaces**:
- `QuizRepository` - Quiz queries
- `QuestionRepository` - Question queries with ordering
- `SessionRepository` - Session lookups by code
- `StudentRepository` - Student queries with ranking
- `AnswerRepository` - Answer analytics and statistics

### 4. Entity/Model Layer
**Purpose**: Domain objects and database mappings

**Entities**:
```
Quiz (1) ─────→ (M) Question
  │                   │
  │                   │ (1)
  │ (1)               │
  │                   ▼
  └────→ (M) Session (M) ─→ (M) Student
                                  │
                                  │ (M)
                                  ▼
                              Answer (M) ─→ (1) Question
```

### 5. DTO Layer
**Purpose**: Data transfer between layers

**Request DTOs**:
- `QuizRequest`
- `QuestionRequest`
- `AnswerRequest`
- `JoinSessionRequest`

**Response DTOs**:
- `QuizResponse`
- `QuestionResponse`
- `SessionResponse`
- `StudentResponse`
- `AnswerStatsResponse`
- `LeaderboardEntry`
- `LeaderboardResponse`

### 6. Exception Handling
**Purpose**: Centralized error management

**Components**:
- `QuizAppException` - Custom exception with error codes
- `ErrorCode` - Enum of all possible error codes
- `ErrorResponse` - Standard error response format
- `GlobalExceptionHandler` - @RestControllerAdvice for centralized handling

### 7. WebSocket Configuration
**Purpose**: Real-time communication setup

**Components**:
- `WebSocketConfig` - STOMP configuration
- `QuizWebSocketHandler` - Message mapping and routing

## 📊 Data Model

### Entity Relationships

```sql
-- Quiz is the root aggregate
Quiz (id, title, description, created_at, updated_at)
  ├─ Questions (1:M)
  │   └─ Answers (1:M)
  │       └─ Students (M:1)
  └─ Sessions (1:M)
      └─ Students (1:M)
          └─ Answers (1:M)
              └─ Questions (M:1)
```

### Key Constraints

1. **Session Code Uniqueness**
   ```sql
   ALTER TABLE sessions ADD CONSTRAINT unique_session_code UNIQUE (code);
   ```

2. **Duplicate Answer Prevention**
   ```sql
   ALTER TABLE answers ADD CONSTRAINT unique_student_question 
   UNIQUE (student_id, question_id);
   ```

3. **Referential Integrity**
   - All foreign keys cascade on delete
   - All relationships use LAZY loading for performance

## 🔄 Request Flow

### Quiz Creation Flow

```
HTTP POST /api/quizzes
    ↓
QuizController.createQuiz()
    ↓
QuizService.createQuiz()
    ├─ Save Quiz entity
    ├─ For each question:
    │   └─ QuestionService.saveQuestion()
    │       └─ Save Question entity with options
    └─ Return Quiz with questions
    ↓
QuizResponse (id, title, description, questionCount)
```

### Session Start Flow

```
HTTP POST /api/sessions/start/{quizId}
    ↓
SessionController.startSession()
    ↓
SessionService.startSession()
    ├─ Generate unique 6-char code (with retry logic)
    ├─ Create Session entity (status = ACTIVE)
    ├─ Fetch Quiz details
    └─ Return Session with Quiz questions
    ↓
SessionResponse (id, code, status, quizId, questions)
```

### Student Join Flow

```
HTTP POST /api/sessions/join/{code}
    ↓
SessionController.joinSession()
    ├─ SessionService.getSessionByCode()
    ├─ Validate session is ACTIVE
    └─ StudentService.saveStudent()
        ├─ Generate UUID for student ID
        └─ Save Student entity
    ↓
StudentResponse (id, studentId, name, joinedAt, correctAnswers)
```

### Answer Submission Flow (WebSocket)

```
WS Message to /app/answer
    ↓
QuizWebSocketHandler.handleStudentAnswer()
    ↓
AnswerService.submitAnswer()
    ├─ SessionService.validateSessionIsActive()
    ├─ Fetch Student and Question
    ├─ Validate option is within range
    ├─ Check for duplicate answer (unique constraint)
    ├─ Create Answer entity
    ├─ Update Student's correct answer count if correct
    └─ Save Answer
    ↓
AnswerService.getQuestionStats()
    ├─ Fetch all answers for question
    ├─ Calculate answer counts
    ├─ Calculate percentages
    ├─ Count correct answers
    └─ Return AnswerStatsResponse
    ↓
Broadcast to /topic/session/{sessionId}
    └─ All connected clients receive updated stats
```

### Leaderboard Generation Flow

```
HTTP GET /api/answers/leaderboard/{sessionId}
    ↓
AnswerController.getLeaderboard()
    ↓
LeaderboardService.getLeaderboard()
    ├─ StudentService.getStudentsBySessionIdOrderedByScore()
    │   └─ Query: SELECT * FROM students WHERE session_id = ? ORDER BY correct_answers DESC
    ├─ For each student:
    │   └─ Create LeaderboardEntry with rank
    └─ Return LeaderboardResponse with sorted entries
    ↓
LeaderboardResponse (sessionId, totalParticipants, leaderboard[])
```

## 🛡️ Business Logic Rules

### Session Code Generation
```java
// Generate 6-character alphanumeric code
// Retry up to 5 times if collision occurs
// Ensure uniqueness at database level
```

### Answer Validation
```
1. Session must be ACTIVE
2. Student must be enrolled in the session
3. Question must exist
4. Selected option must be valid (0 to options.length - 1)
5. Student cannot answer same question twice (unique constraint)
```

### Correctness Determination
```
Answer is correct if:
  selectedOption == question.correctAnswer
```

### Leaderboard Ranking
```
1. Sort by correct_answers DESC
2. Order by joined_at ASC for tie-breaking
3. Assign rank based on sorted position
```

## 🔌 WebSocket Protocol

### Connection Flow

```javascript
// Client connects
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
  // Connected to /ws endpoint
  // Can now subscribe and send messages
});
```

### Message Destinations

| Destination | Type | Purpose |
|---|---|---|
| `/app/answer` | Send | Submit student answer |
| `/app/leaderboard/{id}` | Send | Request leaderboard |
| `/topic/session/{id}` | Subscribe | Receive answer stats |
| `/topic/leaderboard/{id}` | Subscribe | Receive leaderboard updates |

### Message Format

**Answer Submission**:
```json
{
  "studentId": 1,
  "questionId": 1,
  "selectedOption": 0,
  "sessionCode": "ABC123"
}
```

**Answer Stats Response**:
```json
{
  "questionId": 1,
  "questionText": "What is JVM?",
  "totalResponses": 5,
  "answerCounts": {
    "0": 2,
    "1": 1,
    "2": 1,
    "3": 1
  },
  "answerPercentages": {
    "Option A": 40.0,
    "Option B": 20.0,
    "Option C": 20.0,
    "Option D": 20.0
  },
  "correctAnswer": 0,
  "correctCount": 2
}
```

## 🎯 Key Design Decisions

### 1. No Authentication
**Decision**: Use generated student IDs instead of authentication
**Rationale**: Simpler for classroom scenarios, faster onboarding

### 2. Eager Question Option Loading
**Decision**: Questions load all options eagerly
**Rationale**: Options are small and always needed

### 3. Lazy Session/Student/Question Relationships
**Decision**: Use FetchType.LAZY for foreign keys
**Rationale**: Avoid N+1 queries, improve performance

### 4. In-Memory Session Broker
**Decision**: Spring's simple message broker (not external)
**Rationale**: Suitable for small deployments, scales with WebSocket handlers

### 5. H2 Default Database
**Decision**: Use H2 for development, PostgreSQL for production
**Rationale**: Zero setup for development, robust for production

### 6. Cascade Delete
**Decision**: Delete cascade on all relationships
**Rationale**: Simplifies cleanup, prevents orphaned records

### 7. Unique Constraint on Answer
**Decision**: Database-level unique constraint on (student_id, question_id)
**Rationale**: Prevents duplicate answers at database level

## 📈 Performance Considerations

### Query Optimization

```java
// Leaderboard query (optimized)
List<Student> students = studentRepository
  .findBySessionIdOrderByCorrectAnswersDesc(sessionId);
// Single query with ORDER BY clause

// Answer stats calculation
long count = answerRepository
  .countAnswersByQuestionAndOption(questionId, option);
// Uses dedicated @Query with COUNT
```

### Caching Opportunities

```
1. Cache frequently accessed quizzes
2. Cache session state (code → session)
3. Cache student rankings (updated on each answer)
4. Cache question statistics (expire after time)
```

### Database Indexes

```sql
CREATE INDEX idx_session_code ON sessions(code);
CREATE INDEX idx_students_session ON students(session_id);
CREATE INDEX idx_answers_student_question ON answers(student_id, question_id);
CREATE INDEX idx_answers_question ON answers(question_id);
CREATE INDEX idx_questions_quiz ON questions(quiz_id);
CREATE INDEX idx_students_correct_answers ON students(correct_answers DESC);
```

## 🔐 Security Measures

### Input Validation
- @Valid annotations on DTO
- Business logic validation in services
- Database constraints

### SQL Injection Protection
- JPA parameterized queries
- No raw SQL string concatenation

### CORS Configuration
```java
.setAllowedOriginPatterns("*")  // Configure in production
```

### Session Code Security
- 6-character alphanumeric (enough for typical classes)
- Case-insensitive for user convenience
- Collision detection with retry logic

## 🚀 Scalability Path

### Phase 1: Single Instance
- Current setup with H2 or PostgreSQL

### Phase 2: Multi-Instance (Horizontal Scale)
- Replace simple broker with Redis pub/sub
- Shared database (PostgreSQL)
- Load balancer (sticky sessions for WebSocket)

### Phase 3: Microservices
- Quiz Service (quiz management)
- Session Service (session management)
- Analytics Service (statistics)
- WebSocket Gateway (real-time layer)

### Phase 4: Advanced Features
- Redis caching layer
- Kafka for event streaming
- Elastic Search for analytics
- WebSocket load balancing

## 📝 Configuration Profiles

### Development (H2)
```yaml
datasource.url: jdbc:h2:mem:testdb
jpa.hibernate.ddl-auto: create-drop
```

### Production (PostgreSQL)
```yaml
datasource.url: jdbc:postgresql://localhost:5432/quiz_db
jpa.hibernate.ddl-auto: update
```

## 🧪 Testing Strategy

### Unit Tests
- Test services in isolation
- Mock repositories
- Test business logic

### Integration Tests
- Test full flow with real database
- Test REST endpoints
- Test WebSocket handlers

### Load Testing
- Test WebSocket connection limits
- Test database connection pool
- Test message throughput

## 📚 Dependencies Management

```
Spring Boot 3.2
├─ Spring Web
├─ Spring Data JPA
├─ Spring WebSocket
├─ PostgreSQL Driver
├─ H2 Database
├─ Lombok
├─ Jackson
└─ Apache Commons
```

## ✅ Deployment Checklist

- [ ] Switch to PostgreSQL
- [ ] Configure production properties
- [ ] Enable HTTPS
- [ ] Set up monitoring
- [ ] Configure CORS properly
- [ ] Add rate limiting
- [ ] Set up logging
- [ ] Test load scenarios
- [ ] Create backup strategy
- [ ] Set up CI/CD pipeline
