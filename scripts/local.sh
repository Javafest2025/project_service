#!/bin/bash

# Project Service Local Development Script
# This script provides commands to build, run, test, and manage the Spring Boot project service

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="project_service"
JAR_NAME="project_service-0.0.1-SNAPSHOT.jar"
DEFAULT_PORT=8083
PID_FILE="project_service.pid"
LOG_FILE="project_service.log"
MAVEN_CMD="mvn"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Java is installed
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt "21" ]; then
        print_error "Java 21 or higher is required. Current version: $JAVA_VERSION"
        exit 1
    fi
    
    print_success "Java version: $(java -version 2>&1 | head -n 1)"
}

# Function to check if Maven is installed
check_maven() {
    # First try to use mvn command
    if command -v mvn &> /dev/null; then
        MAVEN_CMD="mvn"
        print_success "Using system Maven: $(mvn -version | head -n 1)"
    # Fallback to Maven wrapper if available
    elif test -f ./mvnw; then
        MAVEN_CMD="./mvnw"
        print_success "Using Maven wrapper: $($MAVEN_CMD -version | head -n 1)"
    else
        print_error "Neither Maven (mvn) nor Maven wrapper (./mvnw) is available"
        print_error "Please install Maven or ensure ./mvnw exists in the project"
        exit 1
    fi
}

# Function to format code
format() {
    print_status "Formatting code..."
    
    # Check if spotless is available
    if $MAVEN_CMD help:evaluate -Dexpression=plugin.artifactId -q -DforceStdout | grep -q "spotless-maven-plugin"; then
        if ! $MAVEN_CMD spotless:check; then
            print_status "Applying code format..."
            $MAVEN_CMD spotless:apply || {
                print_error "Formatting failed."
                exit 1
            }
        else
            print_success "Code format is up to date."
        fi
    else
        print_warning "Spotless plugin not found. Skipping code formatting."
    fi
}

# Function to build the application
build() {
    print_status "Building $APP_NAME..."
    
    # Format code first
    format
    
    # Clean and compile
    $MAVEN_CMD clean compile
    
    # Run tests
    $MAVEN_CMD test
    
    # Package the application
    $MAVEN_CMD package -DskipTests
    
    print_success "Build completed successfully!"
}

# Function to run tests
test() {
    print_status "Running tests..."
    $MAVEN_CMD test
    print_success "Tests completed!"
}

# Function to run the application
run() {
    local port=${1:-$DEFAULT_PORT}
    
    print_status "Starting $APP_NAME on port $port..."
    
    # Check if application is already running
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            print_warning "Application is already running with PID $pid"
            print_status "Use './scripts/local.sh stop' to stop it first"
            return 1
        else
            rm -f "$PID_FILE"
        fi
    fi
    
    # Start the application
    nohup $MAVEN_CMD spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=$port" > "$LOG_FILE" 2>&1 &
    local pid=$!
    echo $pid > "$PID_FILE"
    
    print_success "Application started with PID $pid"
    print_status "Logs are being written to $LOG_FILE"
    print_status "API will be available at http://localhost:$port"
    print_status "Swagger UI will be available at http://localhost:$port/docs"
    
    # Wait a moment and check if it started successfully
    sleep 5
    if ! ps -p "$pid" > /dev/null 2>&1; then
        print_error "Application failed to start. Check logs:"
        tail -n 20 "$LOG_FILE"
        rm -f "$PID_FILE"
        exit 1
    fi
    
    print_success "Application is running successfully!"
}

# Function to stop the application
stop() {
    print_status "Stopping $APP_NAME..."
    
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            kill "$pid"
            print_success "Application stopped (PID: $pid)"
        else
            print_warning "Application was not running"
        fi
        rm -f "$PID_FILE"
    else
        print_warning "No PID file found. Application may not be running."
    fi
}

# Function to restart the application
restart() {
    local port=${1:-$DEFAULT_PORT}
    print_status "Restarting $APP_NAME..."
    stop
    sleep 2
    run "$port"
}

# Function to check application status
status() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            print_success "Application is running (PID: $pid)"
            print_status "API: http://localhost:$DEFAULT_PORT"
            print_status "Swagger UI: http://localhost:$DEFAULT_PORT/docs"
        else
            print_warning "PID file exists but application is not running"
            rm -f "$PID_FILE"
        fi
    else
        print_warning "Application is not running"
    fi
}

# Function to show logs
logs() {
    if [ -f "$LOG_FILE" ]; then
        tail -f "$LOG_FILE"
    else
        print_warning "No log file found. Application may not have been started."
    fi
}

# Function to clean up
clean() {
    print_status "Cleaning up..."
    stop
    $MAVEN_CMD clean
    rm -f "$LOG_FILE"
    print_success "Cleanup completed!"
}

# Function to show help
show_help() {
    echo "Project Service Local Development Script"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  format                   Format code using Spotless"
    echo "  build                    Build the application (format, clean, compile, test, package)"
    echo "  test                     Run tests only"
    echo "  run [PORT]              Start the application (default port: $DEFAULT_PORT)"
    echo "  stop                    Stop the application"
    echo "  restart [PORT]          Restart the application"
    echo "  status                  Show application status"
    echo "  logs                    Show application logs (follow mode)"
    echo "  clean                   Stop app, clean build artifacts and logs"
    echo "  help                    Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 format"
    echo "  $0 build"
    echo "  $0 test"
    echo "  $0 run"
    echo "  $0 run 8084"
    echo "  $0 restart"
    echo "  $0 logs"
}

# Main script logic
main() {
    # Change to project root directory
    cd "$(dirname "$0")/.."
    
    # Check prerequisites
    check_java
    check_maven
    
    case "${1:-help}" in
        "format")
            format
            ;;
        "build")
            build
            ;;
        "test")
            test
            ;;
        "run")
            run "$2"
            ;;
        "stop")
            stop
            ;;
        "restart")
            restart "$2"
            ;;
        "status")
            status
            ;;
        "logs")
            logs
            ;;
        "clean")
            clean
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            print_error "Unknown command: $1"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"
