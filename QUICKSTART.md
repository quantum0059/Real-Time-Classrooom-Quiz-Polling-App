# Quick Start Commands

## Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 12+ (optional, H2 included for dev)

## Build & Run

### Development (H2 In-Memory Database)
```bash
cd /home/quantum/Documents/FullStackExam/quizapp
mvn clean install
mvn spring-boot:run
```

### Production (PostgreSQL)
```bash
# Create database
createdb quiz_db

# Run with PostgreSQL profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

## API Testing

### Load Sample Data
```bash
bash sample-data-loader.sh
```

### Run All API Tests
```bash
bash api-test.sh
bash api-test.sh http://localhost:9090/api  # Custom port
```

### Manual API Testing

#### Create Quiz
```bash
curl -X POST http://localhost:8080/api/quizzes \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Sample Quiz",
    "description": "Sample Description",
    "questions": [
      {
        "text": "Question 1?",
        "options": ["A", "B", "C", "D"],
        "correctAnswer": 0,
        "questionOrder": 0
      }
    ]
  }'
```

#### Start Session
```bash
curl -X POST http://localhost:8080/api/sessions/start/1
```

#### Join Session
```bash
curl -X POST http://localhost:8080/api/sessions/join/ABC123 \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe"}'
```

#### Submit Answer
```bash
curl -X POST http://localhost:8080/api/answers \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "questionId": 1,
    "selectedOption": 0,
    "sessionCode": "ABC123"
  }'
```

#### Get Question Stats
```bash
curl http://localhost:8080/api/answers/stats/question/1
```

#### Get Leaderboard
```bash
curl http://localhost:8080/api/answers/leaderboard/1
```

## WebSocket Testing

### Using WebSocket Client (HTML)
Open `websocket-client.html` in browser

### Using curl
```bash
curl -N -H "Connection: Upgrade" -H "Upgrade: websocket" \
  http://localhost:8080/ws
```

### Using JavaScript
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
  console.log('Connected: ' + frame.headers['server']);
  
  // Subscribe to updates
  stompClient.subscribe('/topic/session/1', (message) => {
    console.log('Stats:', JSON.parse(message.body));
  });
  
  // Send answer
  stompClient.send('/app/answer', {}, JSON.stringify({
    studentId: 1,
    questionId: 1,
    selectedOption: 0,
    sessionCode: 'ABC123'
  }));
});
```

## Testing

### Run Unit Tests
```bash
mvn test
```

### Run All Tests
```bash
mvn test -DargLine="-Xmx512m"
```

### Run Specific Test
```bash
mvn test -Dtest=QuizServiceTest
```

## Database Access

### H2 Console (Development)
- URL: http://localhost:8080/h2-console
- Driver: org.h2.Driver
- URL: jdbc:h2:mem:testdb
- Username: sa
- Password: (leave blank)

## Common Issues

### WebSocket Connection Refused
- Ensure application is running on port 8080
- Check CORS configuration in WebSocketConfig
- Verify browser supports WebSocket

### Database Connection Error
- For PostgreSQL: Ensure PostgreSQL is running
- Check connection string in application-prod.yml
- Verify username/password

### Port Already in Use
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Use different port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
```

## Project Structure
```
quizapp/
├── pom.xml                          # Maven configuration
├── README.md                        # Full documentation
├── src/
│   ├── main/
│   │   ├── java/com/quizapp/
│   │   │   ├── QuizAppApplication.java
│   │   │   ├── controller/          # REST endpoints
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # Data access
│   │   │   ├── model/               # JPA entities
│   │   │   ├── dto/                 # Data transfer objects
│   │   │   ├── websocket/           # WebSocket handlers
│   │   │   ├── config/              # Spring configurations
│   │   │   └── exception/           # Exception handling
│   │   └── resources/
│   │       ├── application.yml      # Dev config (H2)
│   │       └── application-prod.yml # Prod config (PostgreSQL)
│   └── test/
│       └── java/com/quizapp/
│           ├── controller/          # Controller tests
│           └── service/             # Service tests
├── sample-data-loader.sh            # Load sample data
├── api-test.sh                      # API testing script
├── quick-start.sh                   # Quick start script
├── websocket-client.html            # WebSocket test client
└── QUICKSTART.md                    # This file
```

## Next Steps

1. **Start the application**
   ```bash
   mvn spring-boot:run
   ```

2. **Load sample data**
   ```bash
   bash sample-data-loader.sh
   ```

3. **Open WebSocket client**
   ```bash
   Open websocket-client.html in browser
   ```

4. **Test APIs**
   ```bash
   bash api-test.sh
   ```

## Documentation

- Full API documentation: See README.md
- Database schema: See README.md
- Error codes: See README.md
- WebSocket protocol: See README.md

## Performance Tips

- Use pagination for large datasets
- Cache frequently accessed data (Redis)
- Use database indexes on session code and student ID
- Configure connection pool size based on load
- Monitor WebSocket connections

## Production Deployment

See README.md for complete production checklist.
