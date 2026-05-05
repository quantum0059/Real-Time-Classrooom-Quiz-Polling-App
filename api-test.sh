#!/bin/bash

# API Testing Script - Test all endpoints
# Usage: ./api-test.sh [base_url]

BASE_URL="${1:-http://localhost:8080/api}"

echo "========================================"
echo "Quiz App - API Testing Script"
echo "Base URL: $BASE_URL"
echo "========================================"
echo ""

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

# Helper function to print section headers
print_section() {
    echo -e "${BLUE}>>> $1${NC}"
}

# Helper function to print success
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Test 1: Create Quiz
print_section "1. Creating Quiz..."
QUIZ_RESPONSE=$(curl -s -X POST "$BASE_URL/quizzes" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "General Knowledge",
    "description": "Test your knowledge",
    "questions": [
      {
        "text": "What is 2 + 2?",
        "options": ["3", "4", "5", "6"],
        "correctAnswer": 1,
        "questionOrder": 0
      },
      {
        "text": "Capital of France?",
        "options": ["London", "Paris", "Berlin", "Madrid"],
        "correctAnswer": 1,
        "questionOrder": 1
      },
      {
        "text": "Largest planet?",
        "options": ["Earth", "Mars", "Jupiter", "Saturn"],
        "correctAnswer": 2,
        "questionOrder": 2
      }
    ]
  }')

QUIZ_ID=$(echo "$QUIZ_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
print_success "Quiz created with ID: $QUIZ_ID"
echo "Response: $QUIZ_RESPONSE"
echo ""

# Test 2: Get Quiz
print_section "2. Getting Quiz Details..."
curl -s "$BASE_URL/quizzes/$QUIZ_ID" | python3 -m json.tool
echo ""

# Test 3: Start Session
print_section "3. Starting Quiz Session..."
SESSION_RESPONSE=$(curl -s -X POST "$BASE_URL/sessions/start/$QUIZ_ID")
SESSION_ID=$(echo "$SESSION_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
SESSION_CODE=$(echo "$SESSION_RESPONSE" | grep -o '"code":"[^"]*' | cut -d'"' -f4)
print_success "Session started with code: $SESSION_CODE (ID: $SESSION_ID)"
echo "Response: $SESSION_RESPONSE"
echo ""

# Test 4: Join Session with Students
print_section "4. Students Joining Session..."
for i in {1..3}; do
  STUDENT_RESPONSE=$(curl -s -X POST "$BASE_URL/sessions/join/$SESSION_CODE" \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"Student $i\"}")
  
  STUDENT_ID=$(echo "$STUDENT_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
  print_success "Student $i joined (ID: $STUDENT_ID)"
  
  # Store for later use
  if [ $i -eq 1 ]; then STUDENT_1_ID=$STUDENT_ID; fi
  if [ $i -eq 2 ]; then STUDENT_2_ID=$STUDENT_ID; fi
done
echo ""

# Test 5: Get Session Details
print_section "5. Getting Session Details..."
curl -s "$BASE_URL/sessions/$SESSION_CODE" | python3 -m json.tool
echo ""

# Test 6: Get Session Students
print_section "6. Getting Session Students..."
curl -s "$BASE_URL/sessions/$SESSION_CODE/students" | python3 -m json.tool
echo ""

# Test 7: Submit Answers
print_section "7. Students Submitting Answers..."

# Get first question ID (should be 1 if sequential)
QUESTION_ID=$((QUIZ_ID * 3 - 2))  # Rough estimate

print_section "Submitting answer from Student 1..."
curl -s -X POST "$BASE_URL/answers" \
  -H "Content-Type: application/json" \
  -d "{
    \"studentId\": $STUDENT_1_ID,
    \"questionId\": $QUESTION_ID,
    \"selectedOption\": 1,
    \"sessionCode\": \"$SESSION_CODE\"
  }" | python3 -m json.tool

print_success "Answer submitted"
echo ""

# Test 8: Get Question Stats
print_section "8. Getting Question Statistics..."
curl -s "$BASE_URL/answers/stats/question/$QUESTION_ID" | python3 -m json.tool
echo ""

# Test 9: Get Leaderboard
print_section "9. Getting Leaderboard..."
curl -s "$BASE_URL/answers/leaderboard/$SESSION_ID" | python3 -m json.tool
echo ""

# Test 10: List All Quizzes
print_section "10. Listing All Quizzes..."
curl -s "$BASE_URL/quizzes" | python3 -m json.tool
echo ""

print_section "API Testing Complete!"
echo ""
echo "Summary:"
echo "  Quiz ID: $QUIZ_ID"
echo "  Session Code: $SESSION_CODE"
echo "  Session ID: $SESSION_ID"
echo "  Student 1 ID: $STUDENT_1_ID"
echo "  Student 2 ID: $STUDENT_2_ID"
echo "  Question ID: $QUESTION_ID"
