package com.ghiloufi.aicode.core.security.config;

import com.ghiloufi.aicode.core.security.model.SafePattern;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SafePatternsConfig {

  @Bean
  public List<SafePattern> safePatterns() {
    return List.of(
        SafePattern.create(
            "PARAMETERIZED_SQL",
            "(?i)PreparedStatement\\s+\\w+\\s*=\\s*.*\\.prepareStatement",
            "Uses PreparedStatement for SQL queries, preventing SQL injection."),
        SafePattern.create(
            "SECURE_RANDOM",
            "(?i)new\\s+SecureRandom\\s*\\(",
            "Uses SecureRandom for cryptographic operations."),
        SafePattern.create(
            "STRONG_CRYPTO_AES",
            "(?i)Cipher\\.getInstance\\s*\\(\\s*[\"']AES",
            "Uses AES encryption algorithm."),
        SafePattern.create(
            "STRONG_HASH_SHA256",
            "(?i)MessageDigest\\.getInstance\\s*\\(\\s*[\"']SHA-256",
            "Uses SHA-256 hashing algorithm."),
        SafePattern.create(
            "PATH_NORMALIZATION",
            "(?i)\\.normalize\\s*\\(\\s*\\)",
            "Normalizes file paths to prevent path traversal."),
        SafePattern.create(
            "XXE_PROTECTION",
            "(?i)setFeature\\s*\\(.*XMLConstants\\.FEATURE_SECURE_PROCESSING",
            "Enables XXE protection on XML parser."),
        SafePattern.create(
            "INPUT_VALIDATION",
            "(?i)(validate|sanitize|escape)\\s*\\(",
            "Uses input validation or sanitization functions."),
        SafePattern.create(
            "PARAMETERIZED_QUERIES",
            "(?i)\\?\\s*,\\s*\\?|setString\\s*\\(|setInt\\s*\\(",
            "Uses parameterized query placeholders."));
  }
}
