# Security Analysis API

REST API for analyzing Java code security vulnerabilities using SpotBugs and Find Security Bugs.

## Features

- **Security Analysis**: Detect OWASP Top 10 vulnerabilities in Java code
- **SpotBugs Integration**: 130+ security detectors from Find Security Bugs
- **REST API**: Simple HTTP endpoint for code analysis
- **Health Checks**: Production-ready health monitoring
- **API Documentation**: Interactive Swagger UI for API exploration

## Quick Start

### Prerequisites

- Java 21 or later
- Maven 3.6 or later

### Build

```bash
mvn clean package
```

### Run

```bash
# Development mode (with Swagger UI)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production mode (Swagger disabled)
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

The API will be available at `http://localhost:8081`

## API Usage

### Analyze Java Code

**Endpoint:** `POST /api/security/analyze`

**Request:**
```json
{
  "code": "package com.example;\nimport java.sql.*;\npublic class VulnerableDAO {\n  public void getUserData(String userId) throws SQLException {\n    Connection conn = DriverManager.getConnection(\"jdbc:h2:mem:test\");\n    Statement stmt = conn.createStatement();\n    String query = \"SELECT * FROM users WHERE id = '\" + userId + \"'\";\n    ResultSet rs = stmt.executeQuery(query);\n  }\n}",
  "language": "java",
  "filename": "VulnerableDAO.java"
}
```

**Response:**
```json
{
  "tool": "SpotBugs",
  "toolVersion": "4.8.3",
  "findings": [
    {
      "type": "SQL_INJECTION_JDBC",
      "severity": "HIGH",
      "line": 7,
      "message": "This dynamic SQL query may be vulnerable to SQL injection",
      "recommendation": "Review and remediate security issue according to OWASP guidelines",
      "cweId": "CWE-89",
      "owaspCategory": "A03:2021-Injection"
    }
  ],
  "analysisTimeMs": 1234
}
```

### cURL Example

```bash
curl -X POST http://localhost:8081/api/security/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Test { private static final String PASSWORD = \"admin123\"; }",
    "language": "java",
    "filename": "Test.java"
  }'
```

## API Documentation

### Swagger UI (Development Only)

When running in development mode, access interactive API documentation at:

```
http://localhost:8081/swagger-ui.html
```

### OpenAPI Specification

JSON specification available at:

```
http://localhost:8081/api-docs
```

## Health Check

Monitor API health status:

```bash
curl http://localhost:8081/actuator/health
```

**Response:**
```json
{
  "status": "UP"
}
```

## Supported Vulnerabilities

The API detects 130+ security issues including:

### OWASP Top 10
- **A01:2021 – Broken Access Control**
- **A02:2021 – Cryptographic Failures** (Weak encryption, insecure hashing)
- **A03:2021 – Injection** (SQL injection, command injection, XPath injection)
- **A04:2021 – Insecure Design**
- **A05:2021 – Security Misconfiguration**
- **A06:2021 – Vulnerable Components**
- **A07:2021 – Authentication Failures** (Hardcoded credentials)
- **A08:2021 – Data Integrity Failures**
- **A09:2021 – Logging Failures**
- **A10:2021 – Server-Side Request Forgery**

### Common Vulnerabilities (CWE)
- CWE-22: Path Traversal
- CWE-78: OS Command Injection
- CWE-79: Cross-Site Scripting (XSS)
- CWE-89: SQL Injection
- CWE-327: Weak Cryptography
- CWE-798: Hardcoded Credentials
- And 120+ more...

## Configuration

### Profiles

- **dev**: Development mode with debug logging and Swagger UI enabled
- **prod**: Production mode with Swagger disabled and minimal logging

### Application Properties

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8081  # Change API port

logging:
  level:
    com.ghiloufi.security: INFO  # Adjust log level
```

## Testing

### Run All Tests

```bash
mvn test
```

### Test Coverage

```bash
mvn jacoco:report
```

View coverage report at `target/site/jacoco/index.html`

**Current coverage: 81%**

## Version

**Current Version:** 0.0.2-SNAPSHOT

**Java Support:** Java 21 LTS only

## License

Internal project - All rights reserved
