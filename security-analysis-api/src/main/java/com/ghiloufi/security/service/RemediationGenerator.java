package com.ghiloufi.security.service;

import com.ghiloufi.security.model.SecurityFinding;
import org.springframework.stereotype.Service;

@Service
public class RemediationGenerator {

  public String generateRemediation(final SecurityFinding finding, final String code) {
    return switch (finding.type()) {
      case String s when s.contains("SQL_INJECTION") -> generateSqlInjectionFix(finding, code);
      case String s when s.contains("XSS") -> generateXssFix(finding, code);
      case String s when s.contains("HARDCODED") -> generateHardcodedSecretFix(finding, code);
      case String s when s.contains("WEAK_CIPHER") || s.contains("WEAK_MESSAGE_DIGEST") ->
          generateWeakCryptoFix(finding, code);
      default -> generateDefaultRemediation(finding);
    };
  }

  private String generateSqlInjectionFix(final SecurityFinding finding, final String code) {
    final String queryVar = extractQueryVariableName(code);

    return String.format(
        """
        Replace with PreparedStatement:

        ```java
        String %s = "SELECT * FROM users WHERE id = ?";
        PreparedStatement stmt = conn.prepareStatement(%s);
        stmt.setString(1, userId);
        ResultSet rs = stmt.executeQuery();
        ```

        References:
        - OWASP SQL Injection Prevention: https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html
        - CWE-89: https://cwe.mitre.org/data/definitions/89.html
        """,
        queryVar, queryVar);
  }

  private String generateXssFix(final SecurityFinding finding, final String code) {
    return """
        Escape output to prevent XSS:

        ```java
        // Option 1: Use Thymeleaf with auto-escaping
        <p th:text="${userInput}">Default text</p>

        // Option 2: Use StringEscapeUtils (Apache Commons Text)
        import org.apache.commons.text.StringEscapeUtils;
        String safe = StringEscapeUtils.escapeHtml4(userInput);

        // Option 3: Use OWASP ESAPI
        import org.owasp.esapi.ESAPI;
        String safe = ESAPI.encoder().encodeForHTML(userInput);
        ```

        References:
        - OWASP XSS Prevention: https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html
        - CWE-79: https://cwe.mitre.org/data/definitions/79.html
        """;
  }

  private String generateHardcodedSecretFix(final SecurityFinding finding, final String code) {
    return """
        Use environment variables or secure configuration:

        ```java
        // Option 1: Environment variables
        String password = System.getenv("DB_PASSWORD");
        String apiKey = System.getenv("API_KEY");

        // Option 2: Spring @Value annotation
        @Value("${database.password}")
        private String password;

        @Value("${api.key}")
        private String apiKey;

        // Option 3: application.properties
        database.password=${DB_PASSWORD}
        api.key=${API_KEY}
        ```

        Best Practices:
        - Never commit secrets to version control
        - Use secret management tools (HashiCorp Vault, AWS Secrets Manager)
        - Rotate secrets regularly

        References:
        - OWASP Secrets Management: https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html
        - CWE-798: https://cwe.mitre.org/data/definitions/798.html
        """;
  }

  private String generateWeakCryptoFix(final SecurityFinding finding, final String code) {
    return """
        Replace weak cipher with strong modern alternative:

        ```java
        // INSECURE: DES, RC4, RC2, MD5, SHA1
        Cipher cipher = Cipher.getInstance("DES");
        MessageDigest md = MessageDigest.getInstance("MD5");

        // SECURE: AES-256-GCM with proper IV
        import javax.crypto.*;
        import javax.crypto.spec.GCMParameterSpec;
        import java.security.SecureRandom;

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // 256-bit AES
        SecretKey key = keyGen.generateKey();

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        // For hashing: Use SHA-256 or stronger
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        ```

        References:
        - OWASP Cryptographic Storage: https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html
        - CWE-327: https://cwe.mitre.org/data/definitions/327.html
        """;
  }

  private String generateDefaultRemediation(final SecurityFinding finding) {
    return String.format(
        """
        Review OWASP guidelines for %s

        References:
        - OWASP Top 10: https://owasp.org/www-project-top-ten/
        - CWE Details: https://cwe.mitre.org/data/definitions/%s.html
        """,
        finding.owaspCategory(), finding.cweId().replace("CWE-", ""));
  }

  private String extractQueryVariableName(final String code) {
    if (code == null) {
      return "query";
    }

    final String[] lines = code.split("\n");
    for (final String line : lines) {
      if (line.contains("String") && (line.contains("SELECT") || line.contains("query"))) {
        final String trimmed = line.trim();
        if (trimmed.startsWith("String")) {
          final String[] parts = trimmed.split("\\s+");
          if (parts.length >= 2) {
            return parts[1].replace("=", "").trim();
          }
        }
      }
    }

    return "query";
  }
}
