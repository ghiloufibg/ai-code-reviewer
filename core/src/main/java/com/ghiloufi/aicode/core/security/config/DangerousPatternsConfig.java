package com.ghiloufi.aicode.core.security.config;

import com.ghiloufi.aicode.core.security.model.DangerousPattern;
import com.ghiloufi.aicode.core.security.model.Severity;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DangerousPatternsConfig {

  @Bean
  public List<DangerousPattern> dangerousPatterns() {
    return List.of(
        DangerousPattern.create(
            "SQL_INJECTION",
            "(?i)(SELECT|INSERT|UPDATE|DELETE)\\s+[^;]*\\+",
            Severity.CRITICAL,
            "SQL query with string concatenation detected. This can lead to SQL injection attacks.",
            "Use PreparedStatement with parameterized queries instead of string concatenation."),
        DangerousPattern.create(
            "HARDCODED_CREDENTIALS",
            "(?i)(password|secret|api[_-]?key|token)\\s*=\\s*([\"'][^\"']{8,}[\"']|[^\\s\"']{8,})",
            Severity.CRITICAL,
            "Hardcoded credentials detected in code. This is a severe security vulnerability.",
            "Use environment variables or secure credential management systems."),
        DangerousPattern.create(
            "EVAL_USAGE",
            "(?i)\\beval\\s*\\(",
            Severity.CRITICAL,
            "eval() function usage detected. This allows arbitrary code execution.",
            "Avoid eval() and use safer alternatives like JSON.parse() or specific parsers."),
        DangerousPattern.create(
            "UNSAFE_DESERIALIZATION",
            "(?i)(readObject|readUnshared|ObjectInputStream)\\s*\\(",
            Severity.HIGH,
            "Unsafe deserialization detected. This can lead to remote code execution.",
            "Validate and sanitize serialized data, or use safer serialization formats like JSON."),
        DangerousPattern.create(
            "WEAK_CRYPTO",
            "(?i)(DES|MD5|SHA-?1)[\"')]|(MessageDigest\\.getInstance\\s*\\(\\s*[\"'](MD5|SHA-?1)[\"'])",
            Severity.HIGH,
            "Weak cryptographic algorithm detected. These algorithms are no longer secure.",
            "Use AES-256 for encryption and SHA-256 or better for hashing."),
        DangerousPattern.create(
            "INSECURE_RANDOM",
            "(?i)new\\s+Random\\s*\\(",
            Severity.MEDIUM,
            "Insecure random number generator detected. Not suitable for security purposes.",
            "Use SecureRandom for cryptographic operations and security-sensitive code."),
        DangerousPattern.create(
            "XXE_VULNERABILITY",
            "(?i)DocumentBuilderFactory\\.newInstance\\(\\)(?!.*setFeature)",
            Severity.HIGH,
            "XML parser without XXE protection. This can lead to XML External Entity attacks.",
            "Disable external entity processing using setFeature() on the parser factory."),
        DangerousPattern.create(
            "XPATH_INJECTION",
            "(?i)[\"\\']/.*?\\+|XPath\\.compile\\s*\\([^)]*\\+",
            Severity.CRITICAL,
            "XPath expression with string concatenation. This can lead to XPath injection.",
            "Use parameterized XPath expressions or validate input thoroughly."),
        DangerousPattern.create(
            "LDAP_INJECTION",
            "(?i)(search|lookup)\\s*\\([^)]*\\+[^)]*\\)",
            Severity.HIGH,
            "LDAP query with string concatenation. This can lead to LDAP injection.",
            "Use LDAP parameterized queries and escape special characters."),
        DangerousPattern.create(
            "NULL_CIPHER",
            "(?i)Cipher\\.getInstance\\s*\\(\\s*[\"'].*NULL.*[\"']\\s*\\)",
            Severity.CRITICAL,
            "NULL cipher usage detected. This provides no encryption at all.",
            "Use strong encryption algorithms like AES with appropriate modes."),
        DangerousPattern.create(
            "COMMAND_INJECTION",
            "(?i)Runtime\\.getRuntime\\(\\)\\.exec\\s*\\((?!\\s*new\\s+String\\[)",
            Severity.CRITICAL,
            "Runtime.exec() usage detected. This can lead to command injection if user input is involved.",
            "Use ProcessBuilder with separate arguments and validate all inputs."));
  }
}
