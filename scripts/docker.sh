#!/bin/bash

# Project Service Docker Management Script
# This script provides commands to build, run, test, and manage the Docker containerized Project Service

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
CONTAINER_NAME="scholar-project-service"
IMAGE_NAME="scholar-project-service:latest"
NETWORK_NAME="scholarai-network"
DEFAULT_PORT=8083

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

# Function to check if Docker is installed
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed or not in PATH"
        exit 1
    fi
    
    print_success "Docker and Docker Compose are available"
}

# Function to create network if it doesn't exist
create_network() {
    if ! docker network ls | grep -q "$NETWORK_NAME"; then
        print_status "Creating Docker network: $NETWORK_NAME"
        docker network create "$NETWORK_NAME"
        print_success "Network $NETWORK_NAME created"
    else
        print_status "Network $NETWORK_NAME already exists"
    fi
}

# Function to build the Docker image
build() {
    print_status "Building Docker image for $APP_NAME..."
    
    create_network
    
    # Build the image
    docker-compose build
    
    print_success "Docker image built successfully!"
}

# Function to rebuild the Docker image without cache
rebuild_nocache() {
    print_status "Rebuilding Docker image for $APP_NAME without cache..."
    
    create_network
    
    # Remove existing image first
    docker-compose down --rmi all
    
    # Build fresh image without cache
    docker-compose build --no-cache --pull
    
    print_success "Docker image rebuilt successfully without cache!"
}

# Function to run the application
run() {
    print_status "Starting $APP_NAME container with dependencies..."
    
    # Check if container is already running
    if docker ps | grep -q "$CONTAINER_NAME"; then
        print_warning "Container $CONTAINER_NAME is already running"
        print_status "Use 'docker.sh stop' to stop it first"
        return 1
    fi
    
    create_network
    
    # Start the container with dependencies
    docker-compose up -d
    
    print_success "Container started successfully!"
    print_status "Project Service is available at http://localhost:$DEFAULT_PORT"
    print_status "PostgreSQL is available at localhost:5435"
    
    # Wait a moment and check if it started successfully
    sleep 20
    if ! docker ps | grep -q "$CONTAINER_NAME"; then
        print_error "Container failed to start. Check logs:"
        docker-compose logs
        exit 1
    fi
    
    print_success "Project Service is running successfully!"
}

# Function to stop the application
stop() {
    print_status "Stopping $APP_NAME container and dependencies..."
    
    docker-compose down
    
    print_success "Container stopped successfully!"
}

# Function to restart the application
restart() {
    print_status "Restarting $APP_NAME container..."
    stop
    sleep 2
    run
}

# Function to check application status
status() {
    if docker ps | grep -q "$CONTAINER_NAME"; then
        print_success "Container $CONTAINER_NAME is running"
        print_status "Project Service: http://localhost:$DEFAULT_PORT"
        docker ps --filter "name=scholar-project" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    else
        print_warning "Container $CONTAINER_NAME is not running"
    fi
}

# Function to show logs
logs() {
    if docker ps -a | grep -q "$CONTAINER_NAME"; then
        docker-compose logs -f project-service
    else
        print_warning "Container $CONTAINER_NAME does not exist"
    fi
}

# Function to clean up
clean() {
    print_status "Cleaning up..."
    stop
    
    # Remove containers if they exist
    docker-compose down --rmi all --volumes
    
    print_success "Cleanup completed!"
}

# Function to show help
show_help() {
    echo "Project Service Docker Management Script"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  build                    Build the Docker image"
    echo "  rebuild-nocache          Rebuild the Docker image without cache"
    echo "  run                      Start the container with dependencies"
    echo "  stop                     Stop the container and dependencies"
    echo "  restart                  Restart the container"
    echo "  status                   Show container status"
    echo "  logs                     Show container logs (follow mode)"
    echo "  clean                    Stop containers, remove containers, images and volumes"
    echo "  help                     Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 build"
    echo "  $0 run"
    echo "  $0 logs"
}

# Main script logic
main() {
    # Change to project root directory
    cd "$(dirname "$0")/.."
    
    # Check prerequisites
    check_docker
    
    case "${1:-help}" in
        "build")
            build
            ;;
        "rebuild-nocache")
            rebuild_nocache
            ;;
        "run")
            run
            ;;
        "stop")
            stop
            ;;
        "restart")
            restart
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
