#!/bin/bash

# Sample Data Loader - Run this after the application starts
# This script populates the database with sample quizzes and questions

API_BASE_URL="http://localhost:8080/api"

echo "========================================"
echo "Quiz App - Sample Data Loader"
echo "========================================"

# Create a sample quiz
echo "Creating sample quiz..."
QUIZ_RESPONSE=$(curl -s -X POST "$API_BASE_URL/quizzes" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "General Knowledge Quiz",
    "description": "Test your general knowledge",
    "questions": [
      {
        "text": "What is the capital of France?",
        "options": ["London", "Berlin", "Paris", "Madrid"],
        "correctAnswer": 2,
        "questionOrder": 0
      },
      {
        "text": "Which planet is the largest in our solar system?",
        "options": ["Earth", "Mars", "Jupiter", "Saturn"],
        "correctAnswer": 2,
        "questionOrder": 1
      },
      {
        "text": "What is the chemical symbol for Gold?",
        "options": ["Go", "Gd", "Au", "Ag"],
        "correctAnswer": 2,
        "questionOrder": 2
      },
      {
        "text": "Who wrote Romeo and Juliet?",
        "options": ["Jane Austen", "William Shakespeare", "Charles Dickens", "Mark Twain"],
        "correctAnswer": 1,
        "questionOrder": 3
      }
    ]
  }')

echo "Quiz Response: $QUIZ_RESPONSE"

# Extract quiz ID (assuming jq is available)
QUIZ_ID=$(echo "$QUIZ_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')

if [ -z "$QUIZ_ID" ]; then
  echo "Failed to create quiz"
  exit 1
fi

echo "Quiz created with ID: $QUIZ_ID"

# Start a session
echo ""
echo "Starting quiz session..."
SESSION_RESPONSE=$(curl -s -X POST "$API_BASE_URL/sessions/start/$QUIZ_ID")

echo "Session Response: $SESSION_RESPONSE"

# Extract session code
SESSION_CODE=$(echo "$SESSION_RESPONSE" | grep -o '"code":"[^"]*' | head -1 | sed 's/"code":"//')

if [ -z "$SESSION_CODE" ]; then
  echo "Failed to start session"
  exit 1
fi

echo "Session started with code: $SESSION_CODE"

# Join session with sample students
echo ""
echo "Joining students to session..."

for i in {1..3}; do
  STUDENT_NAME="Student$i"
  STUDENT_RESPONSE=$(curl -s -X POST "$API_BASE_URL/sessions/join/$SESSION_CODE" \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"$STUDENT_NAME\"}")
  
  echo "Student $i joined: $STUDENT_RESPONSE"
done

echo ""
echo "Sample data loaded successfully!"
echo "Quiz ID: $QUIZ_ID"
echo "Session Code: $SESSION_CODE"
echo ""
echo "You can now test the application using this session code."
