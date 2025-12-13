.PHONY: help dev build-dev up-dev down logs test clean vault vault-down vault-init k8s-build k8s-deploy k8s-down k8s-status k8s-logs k8s-logs-worker k8s-port-forward k8s-full

# Default target
help:
	@echo "AI Code Reviewer - Docker Management"
	@echo ""
	@echo "Development Commands:"
	@echo "  make dev           - Rebuild and run in development mode"
	@echo "  make build-dev     - Build development image"
	@echo "  make up-dev        - Start development container"
	@echo "  make down          - Stop all containers"
	@echo "  make logs          - Show container logs"
	@echo "  make test          - Run all tests"
	@echo "  make clean         - Clean all Docker resources"
	@echo ""
	@echo "Vault Commands:"
	@echo "  make vault         - Start Vault server"
	@echo "  make vault-down    - Stop Vault server"
	@echo "  make vault-init    - Initialize Vault secrets from .env"
	@echo ""
	@echo "Kubernetes Commands:"
	@echo "  make k8s-build     - Build images for local K8s"
	@echo "  make k8s-deploy    - Deploy to local K8s"
	@echo "  make k8s-down      - Remove K8s deployment"
	@echo "  make k8s-status    - Check K8s status"
	@echo "  make k8s-logs      - View api-gateway logs"
	@echo "  make k8s-full      - Build and deploy to K8s"
	@echo ""

# ============================================================================
# Development Commands
# ============================================================================

# Development mode (default)
dev: down build-dev up-dev
	@echo "[OK] Development environment ready!"
	@echo "API: http://localhost:8081"
	@echo "Debug port: 5005"

# Build development image
build-dev:
	@echo "[BUILD] Building development image..."
	@docker-compose -f docker-compose.dev.yml build

# Start development container
up-dev:
	@echo "[START] Starting development container..."
	@docker-compose -f docker-compose.dev.yml up -d
	@sleep 5
	@docker-compose -f docker-compose.dev.yml ps

# Stop containers
down:
	@echo "[STOP] Stopping containers..."
	@docker-compose -f docker-compose.dev.yml down 2>nul || cd .

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
	@docker volume prune -f
	@echo "[OK] Cleanup complete!"

# ============================================================================
# Vault Commands
# ============================================================================

# Start Vault server
vault:
	@echo "[VAULT] Starting Vault server..."
	@docker network create ai-reviewer-network-dev 2>/dev/null || true
	@docker-compose -f docker-compose.vault.yml up -d
	@echo "[OK] Vault started!"
	@echo "Vault UI: http://localhost:8200"
	@echo "Token:    dev-root-token"

# Stop Vault server
vault-down:
	@echo "[VAULT] Stopping Vault server..."
	@docker-compose -f docker-compose.vault.yml down

# Initialize Vault secrets from .env file
vault-init:
	@echo "[VAULT] Initializing Vault secrets from .env file..."
	@docker exec -e VAULT_TOKEN=dev-root-token vault-dev vault secrets enable -path=secret kv-v2 2>/dev/null || true
	@docker exec -e VAULT_TOKEN=dev-root-token vault-dev vault kv put secret/ai-code-reviewer/database \
		url="jdbc:postgresql://postgres:5432/ai_code_reviewer" \
		username="$(POSTGRES_USER)" \
		password="$(POSTGRES_PASSWORD)"
	@docker exec -e VAULT_TOKEN=dev-root-token vault-dev vault kv put secret/ai-code-reviewer/llm \
		openai-api-key="$(LLM_API_KEY)" \
		base-url="$(LLM_BASE_URL)" \
		model="$(LLM_MODEL)"
	@docker exec -e VAULT_TOKEN=dev-root-token vault-dev vault kv put secret/ai-code-reviewer/scm \
		gitlab-token="$(GITLAB_TOKEN)" \
		github-token="$(GITHUB_TOKEN)"
	@docker exec -e VAULT_TOKEN=dev-root-token vault-dev vault kv put secret/ai-code-reviewer/webhook \
		api-keys="$(API_KEY_CICD)"
	@docker exec -e VAULT_TOKEN=dev-root-token vault-dev vault kv put secret/ai-code-reviewer/jira \
		token="$(JIRA_TOKEN)" \
		base-url="http://host.docker.internal:8080"
	@echo "[OK] Vault secrets initialized!"

# ============================================================================
# Kubernetes Commands (Local Docker Desktop)
# ============================================================================

# Build images for local Kubernetes
k8s-build:
	@echo "[K8S] Building Docker images for local Kubernetes..."
	@docker build -t ai-code-reviewer/api-gateway:local -f api-gateway/Dockerfile .
	@docker build -t ai-code-reviewer/llm-worker:local -f llm-worker/Dockerfile .
	@echo "[OK] Images built!"

# Deploy to local Kubernetes
k8s-deploy:
	@echo "[K8S] Deploying to local Kubernetes..."
	@kubectl config use-context docker-desktop
	@kubectl apply -k ai-code-reviewer-release/overlays/local
	@echo "[OK] Deployed to Kubernetes!"
	@echo "Run 'make k8s-status' to check deployment status"

# Remove Kubernetes deployment
k8s-down:
	@echo "[K8S] Removing Kubernetes deployment..."
	@kubectl delete namespace ai-reviewer --ignore-not-found=true

# Check Kubernetes status
k8s-status:
	@echo "[K8S] Kubernetes deployment status:"
	@kubectl get all -n ai-reviewer

# View Kubernetes logs (api-gateway)
k8s-logs:
	@kubectl logs -f deployment/api-gateway -n ai-reviewer

# View Kubernetes logs (llm-worker)
k8s-logs-worker:
	@kubectl logs -f deployment/llm-worker -n ai-reviewer

# Port-forward API Gateway
k8s-port-forward:
	@echo "[K8S] Port-forwarding API Gateway to localhost:8081..."
	@kubectl port-forward svc/api-gateway 8081:8080 -n ai-reviewer

# Full local deployment (build + deploy)
k8s-full: k8s-build k8s-deploy
	@echo "[OK] Full local Kubernetes deployment complete!"
