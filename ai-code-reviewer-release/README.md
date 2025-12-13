# Kubernetes Deployment

This directory contains Kubernetes manifests for deploying the AI Code Reviewer to a Kubernetes cluster using Kustomize.

## Directory Structure

```
k8s/
├── base/                          # Base manifests (shared across environments)
│   ├── kustomization.yaml         # Kustomize configuration
│   ├── namespace.yaml             # Namespace definition
│   ├── api-gateway/               # API Gateway workloads
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── hpa.yaml
│   ├── llm-worker/                # LLM Worker workloads
│   │   ├── deployment.yaml
│   │   └── hpa.yaml
│   ├── redis/                     # Redis (for dev/staging)
│   │   └── statefulset.yaml
│   ├── configmaps/                # Application configuration
│   │   └── app-config.yaml
│   ├── secrets/                   # Secret templates (DO NOT COMMIT REAL VALUES)
│   │   └── secrets-template.yaml
│   └── ingress.yaml               # Ingress routing
└── overlays/
    ├── dev/                       # Development environment
    ├── staging/                   # Staging environment
    └── prod/                      # Production environment
        ├── kustomization.yaml
        └── patches/               # Production-specific patches
```

## Quick Start

### Prerequisites

- Kubernetes cluster (1.25+)
- kubectl configured
- Kustomize (built into kubectl 1.14+)
- Container registry access (ghcr.io, ECR, GCR, etc.)

### Deploy to Development/Staging

```bash
# Create secrets first (edit secrets-template.yaml with real values)
kubectl apply -f k8s/base/secrets/secrets-template.yaml

# Deploy base configuration
kubectl apply -k k8s/base
```

### Deploy to Production

```bash
# Edit production overlay with your values:
# - Update image tags in kustomization.yaml
# - Update managed Redis host in configMapGenerator
# - Create sealed secrets or use external-secrets-operator

# Deploy production overlay
kubectl apply -k k8s/overlays/prod
```

## Configuration

### Required Secrets

Before deploying, create the following secrets:

| Secret Name | Keys | Description |
|-------------|------|-------------|
| `database-credentials` | `url`, `username`, `password` | PostgreSQL connection |
| `llm-credentials` | `openai-api-key` | OpenAI API key |
| `scm-credentials` | `gitlab-token`, `github-token` | SCM provider tokens |
| `webhook-credentials` | `api-keys` | Webhook authentication keys |

### ConfigMap Values

The `app-config` ConfigMap contains non-sensitive configuration:

| Key | Default | Description |
|-----|---------|-------------|
| `redis.host` | `redis` | Redis hostname |
| `redis.port` | `6379` | Redis port |
| `llm.provider` | `openai` | LLM provider name |
| `llm.base-url` | `https://api.openai.com/v1` | LLM API base URL |
| `llm.model` | `gpt-4o` | Default LLM model |

## Operations

### View Deployment Status

```bash
kubectl -n ai-reviewer get pods
kubectl -n ai-reviewer get svc
kubectl -n ai-reviewer get hpa
```

### View Logs

```bash
kubectl -n ai-reviewer logs -f deployment/api-gateway
kubectl -n ai-reviewer logs -f deployment/llm-worker
```

### Port Forward (Local Testing)

```bash
kubectl -n ai-reviewer port-forward svc/api-gateway 8080:8080
```

### Scale Manually

```bash
kubectl -n ai-reviewer scale deployment/llm-worker --replicas=5
```

### Rollout Restart

```bash
kubectl -n ai-reviewer rollout restart deployment/api-gateway
kubectl -n ai-reviewer rollout status deployment/api-gateway
```

### Rollback

```bash
# View history
kubectl -n ai-reviewer rollout history deployment/api-gateway

# Rollback to previous
kubectl -n ai-reviewer rollout undo deployment/api-gateway

# Rollback to specific revision
kubectl -n ai-reviewer rollout undo deployment/api-gateway --to-revision=2
```

## Production Recommendations

### Infrastructure

| Component | Recommendation |
|-----------|----------------|
| PostgreSQL | Use managed service (RDS, Cloud SQL) |
| Redis | Use managed service (ElastiCache, Redis Cloud) |
| Ingress | nginx-ingress or cloud-native (ALB, GKE Ingress) |
| Secrets | Sealed Secrets or External Secrets Operator |
| Monitoring | Prometheus + Grafana |
| Logging | EFK/ELK or Loki |

### Security Checklist

- [ ] Network Policies for pod-to-pod communication
- [ ] Pod Security Standards (restricted profile)
- [ ] Secret encryption at rest (KMS)
- [ ] Image scanning in CI/CD (Trivy, Snyk)
- [ ] RBAC for service accounts
- [ ] Resource quotas per namespace

### High Availability

- [ ] Multi-zone deployment (pod anti-affinity already configured)
- [ ] PodDisruptionBudget for api-gateway and llm-worker
- [ ] Managed database with read replicas
- [ ] Redis Cluster or Sentinel mode
