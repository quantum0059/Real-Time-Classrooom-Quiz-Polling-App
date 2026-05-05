#!/bin/bash

# Quick Start Guide for Real-Time Quiz Application

echo "================================"
echo "Real-Time Quiz App - Quick Start"
echo "================================"
echo ""

# Check Java version
echo "Checking Java installation..."
java -version
if [ $? -ne 0 ]; then
    echo "❌ Java not found. Please install Java 17 or higher."
    exit 1
fi
echo "✓ Java found"
echo ""

# Check Maven
echo "Checking Maven installation..."
mvn -version
if [ $? -ne 0 ]; then
    echo "❌ Maven not found. Please install Maven."
    exit 1
fi
echo "✓ Maven found"
echo ""

# Build the project
echo "Building project..."
mvn clean install

if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi
echo "✓ Build successful"
echo ""

# Run the application
echo "Starting application..."
echo "Application will be available at: http://localhost:8080"
echo ""
echo "API Documentation:"
echo "  - Swagger UI: http://localhost:8080/swagger-ui.html"
echo "  - H2 Console (dev): http://localhost:8080/h2-console"
echo "  - WebSocket: ws://localhost:8080/ws"
echo ""
echo "Press Ctrl+C to stop the application"
echo ""

mvn spring-boot:run

