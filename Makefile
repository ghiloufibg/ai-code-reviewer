.PHONY: help dev prod build-dev build-prod up-dev up-prod down logs test clean

# Default target
help:
	@echo "AI Code Reviewer - Docker Management"
	@echo ""
	@echo "Development Commands:"
	@echo "  make dev           - Rebuild and run in development mode"
	@echo "  make build-dev     - Build development image"
	@echo "  make up-dev        - Start development container"
	@echo ""
	@echo "Production Commands:"
	@echo "  make prod          - Rebuild and run in production mode (no cache)"
	@echo "  make build-prod    - Build production image from scratch"
	@echo "  make up-prod       - Start production container"
	@echo ""
	@echo "Utility Commands:"
	@echo "  make down          - Stop all containers"
	@echo "  make logs          - Show container logs (dev)"
	@echo "  make test          - Run all tests"
	@echo "  make clean         - Clean all Docker resources"
	@echo ""

# Development mode (default)
dev: down build-dev up-dev
	@echo "[OK] Development environment ready!"
	@echo "API: http://localhost:8081"
	@echo "Debug port: 5005"

# Production mode
prod: down build-prod up-prod
	@echo "[OK] Production environment ready!"
	@echo "API: http://localhost:8081"

# Build commands
build-dev:
	@echo "[BUILD] Building development image..."
	@docker-compose -f docker-compose.dev.yml build

build-prod:
	@echo "[BUILD] Building production image (no cache)..."
	@docker-compose -f docker-compose.yml build --no-cache

# Start commands
up-dev:
	@echo "[START] Starting development container..."
	@docker-compose -f docker-compose.dev.yml up -d
	@sleep 5
	@docker-compose -f docker-compose.dev.yml ps

up-prod:
	@echo "[START] Starting production container..."
	@docker-compose -f docker-compose.yml up -d
	@sleep 5
	@docker-compose -f docker-compose.yml ps

# Stop containers
down:
	@echo "[STOP] Stopping containers..."
	@docker-compose -f docker-compose.dev.yml down 2>nul || cd .
	@docker-compose -f docker-compose.yml down 2>nul || cd .

# View logs
logs:
	@docker-compose -f docker-compose.dev.yml logs -f

# Run tests
test:
	@echo "[TEST] Running tests..."
	@mvn test

# Clean Docker resources
clean: down
	@echo "[CLEAN] Cleaning Docker resources..."
	@docker rmi ai-code-reviewer-ai-code-reviewer-dev:latest 2>/dev/null || true
	@docker rmi ai-code-reviewer-ai-code-reviewer:latest 2>/dev/null || true
	@docker volume prune -f
	@echo "[OK] Cleanup complete!"
