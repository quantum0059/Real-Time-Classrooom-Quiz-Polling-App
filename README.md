# Real-Time Quiz Application - Backend

A production-ready real-time classroom quiz/polling application built with Spring Boot, featuring WebSocket support for instant result broadcasting.

## 🚀 Features

### Quiz Management
- Create quizzes with multiple questions
- Each question supports multiple choice options
- Automatic question ordering
- Full CRUD operations

### Session Management
- Start quiz sessions with unique 6-character session codes
- Active/Ended session status tracking
- Track current question index
- No authentication required - use generated student IDs

### Real-Time Participation
- Students join sessions using session codes
- Live answer submission via WebSocket
- Prevent duplicate answers per student per question
- Instant result broadcasting

### Live Analytics
- Real-time answer distribution per question
- Participant count tracking
- Leaderboard based on correct answers
- Answer statistics with percentages

## 🛠️ Tech Stack

- **Java 17+** - Language
- **Spring Boot 3.2** - Framework
- **Spring Web** - REST APIs
- **Spring Data JPA** - Data persistence
- **Spring WebSocket (STOMP + SockJS)** - Real-time communication
- **PostgreSQL/H2** - Database
- **Lombok** - Boilerplate reduction
- **Maven** - Build tool

## 📦 Project Structure

```
com.quizapp
├── controller/          # REST API endpoints
├── service/             # Business logic
├── repository/          # Data access layer
├── model/               # JPA entities
├── dto/                 # Data transfer objects
├── websocket/           # WebSocket message handlers
├── config/              # Spring configurations
└── exception/           # Error handling
```

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (or H2 for development)

## 🔧 Installation & Setup

### 1. Clone and Build

```bash
cd /home/quantum/Documents/FullStackExam/quizapp
mvn clean install
```

### 2. Configure Database

For **Development** (H2 - default):
- No configuration needed, uses in-memory H2 database
- Access H2 console: `http://localhost:8080/h2-console`

For **Production** (PostgreSQL):
```bash
# Create database
createdb quiz_db

# Run with PostgreSQL profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

### 3. Run Application

```bash
mvn spring-boot:run
```

Application will start on `http://localhost:8080`

## 📡 API Endpoints

### Quiz Management

#### Create Quiz
```http
POST /api/quizzes
Content-Type: application/json

{
  "title": "Java Fundamentals",
  "description": "Test your Java knowledge",
  "questions": [
    {
      "text": "What is JVM?",
      "options": ["Option A", "Option B", "Option C", "Option D"],
      "correctAnswer": 0,
      "questionOrder": 0
    }
  ]
}
```

#### Get Quiz
```http
GET /api/quizzes/{id}
```

#### List All Quizzes
```http
GET /api/quizzes
```

### Session Management

#### Start Session
```http
POST /api/sessions/start/{quizId}
```

Response:
```json
{
  "id": 1,
  "code": "ABC123",
  "status": "ACTIVE",
  "quizId": 1,
  "createdAt": "2024-01-15T10:30:00",
  "currentQuestionIndex": 0,
  "participantCount": 0
}
```

#### Get Session
```http
GET /api/sessions/{code}
```

#### Join Session
```http
POST /api/sessions/join/{code}
Content-Type: application/json

{
  "name": "John Doe"
}
```

#### Get Session Students
```http
GET /api/sessions/{code}/students
```

### Answer Submission

#### Submit Answer (REST)
```http
POST /api/answers
Content-Type: application/json

{
  "studentId": 1,
  "questionId": 1,
  "selectedOption": 0,
  "sessionCode": "ABC123"
}
```

#### Get Question Stats
```http
GET /api/answers/stats/question/{questionId}
```

Response:
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

#### Get Leaderboard
```http
GET /api/answers/leaderboard/{sessionId}
```

Response:
```json
{
  "sessionId": 1,
  "totalParticipants": 5,
  "leaderboard": [
    {
      "studentId": 1,
      "name": "Alice",
      "correctAnswers": 4,
      "totalAnswered": 4,
      "score": 4.0,
      "rank": 1
    },
    {
      "studentId": 2,
      "name": "Bob",
      "correctAnswers": 3,
      "totalAnswered": 4,
      "score": 3.0,
      "rank": 2
    }
  ]
}
```

## 🔄 WebSocket API

### Connection

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
  console.log('Connected: ' + frame.headers['server']);
  
  // Subscribe to session updates
  stompClient.subscribe('/topic/session/{sessionId}', (message) => {
    const stats = JSON.parse(message.body);
    console.log('Answer Stats:', stats);
  });
});
```

### Submit Answer (WebSocket)

```javascript
const answerRequest = {
  studentId: 1,
  questionId: 1,
  selectedOption: 0,
  sessionCode: "ABC123"
};

stompClient.send("/app/answer", {}, JSON.stringify(answerRequest));
```

### Subscribe to Question Stats

```javascript
// Results published to /topic/session/{sessionId}
stompClient.subscribe('/topic/session/1', (message) => {
  const stats = JSON.parse(message.body);
  console.log('Answer Distribution:', stats.answerCounts);
  console.log('Percentages:', stats.answerPercentages);
});
```

### Get Leaderboard (WebSocket)

```javascript
stompClient.send("/app/leaderboard/1", {}, JSON.stringify({}));

// Subscribe to leaderboard updates
stompClient.subscribe('/topic/leaderboard/1', (message) => {
  const leaderboard = JSON.parse(message.body);
  console.log('Leaderboard:', leaderboard);
});
```

## 📊 Sample Data

Run the data loader script to populate sample quizzes:

```bash
chmod +x sample-data-loader.sh
./sample-data-loader.sh
```

This creates:
- 1 quiz with 4 questions
- 1 active session
- 3 sample students

## 🗄️ Database Schema

### Quiz Entity
```sql
CREATE TABLE quizzes (
  id BIGINT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  description VARCHAR(500),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

### Question Entity
```sql
CREATE TABLE questions (
  id BIGINT PRIMARY KEY,
  quiz_id BIGINT NOT NULL,
  text TEXT NOT NULL,
  correct_answer INT NOT NULL,
  question_order INT,
  FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
);
```

### Session Entity
```sql
CREATE TABLE sessions (
  id BIGINT PRIMARY KEY,
  quiz_id BIGINT NOT NULL,
  code VARCHAR(6) UNIQUE NOT NULL,
  status VARCHAR(50) NOT NULL,
  current_question_index INT DEFAULT 0,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  ended_at TIMESTAMP,
  FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
);
```

### Student Entity
```sql
CREATE TABLE students (
  id BIGINT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  student_id VARCHAR(255),
  name VARCHAR(255) NOT NULL,
  correct_answers INT DEFAULT 0,
  joined_at TIMESTAMP,
  FOREIGN KEY (session_id) REFERENCES sessions(id)
);
```

### Answer Entity
```sql
CREATE TABLE answers (
  id BIGINT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  selected_option INT NOT NULL,
  is_correct BOOLEAN,
  answered_at TIMESTAMP,
  UNIQUE(student_id, question_id),
  FOREIGN KEY (student_id) REFERENCES students(id),
  FOREIGN KEY (question_id) REFERENCES questions(id)
);
```

## ⚠️ Error Handling

The application includes comprehensive error handling with specific error codes:

| Error Code | HTTP Status | Description |
|-----------|------------|-------------|
| QUIZ_NOT_FOUND | 404 | Quiz doesn't exist |
| SESSION_NOT_FOUND | 404 | Session doesn't exist |
| QUESTION_NOT_FOUND | 404 | Question doesn't exist |
| STUDENT_NOT_FOUND | 404 | Student doesn't exist |
| SESSION_INACTIVE | 400 | Session is ended |
| DUPLICATE_ANSWER | 409 | Student already answered |
| INVALID_OPTION | 400 | Invalid answer option |
| INVALID_SESSION_CODE | 400 | Invalid session code |

## 🔐 Security Best Practices

- Validate session is active before processing answers
- Prevent duplicate answers at database level (unique constraint)
- Sanitize user input on student names
- Use transaction boundaries for data consistency
- Implement CORS properly for WebSocket connections

## 🚀 Production Checklist

- [ ] Switch to PostgreSQL database
- [ ] Configure production profile (application-prod.yml)
- [ ] Set up proper logging
- [ ] Enable HTTPS
- [ ] Configure CORS for frontend domain
- [ ] Add rate limiting
- [ ] Implement caching for frequently accessed data
- [ ] Set up monitoring and metrics
- [ ] Configure proper exception handling
- [ ] Test WebSocket connections under load

## 📈 Scaling Considerations

For high-load scenarios:

1. **Redis Caching** - Cache session states and leaderboards
2. **Message Queue** - Use RabbitMQ/Kafka for async processing
3. **Database Optimization** - Add indexes on frequently queried columns
4. **Connection Pooling** - Configure HikariCP connection pool
5. **Load Balancing** - Use Redis pub/sub for multi-instance WebSocket support

## 📝 Testing

Sample WebSocket client test:

```bash
curl -N -H "Connection: Upgrade" -H "Upgrade: websocket" \
  http://localhost:8080/ws
```

Or use WebSocket client libraries:
- JavaScript: `ws` package or browser native WebSocket
- Java: Spring WebSocket Test Utils
- Python: `websocket-client` package

## 📚 API Documentation

Full Swagger/OpenAPI documentation available at:
```
http://localhost:8080/swagger-ui.html
```

## 📄 License

MIT License - Feel free to use this for educational and commercial projects.

## 👥 Contributing

Contributions are welcome! Please ensure code follows Spring Boot best practices and include unit tests.

## 📞 Support

For issues or questions, refer to the error codes in the exception handling section or check application logs.
