package com.ghiloufi.security.e2e;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Security Analysis E2E Integration Test")
class SecurityAnalysisE2ETest {

  @LocalServerPort private int port;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    RestAssured.baseURI = "http://localhost";
  }

  private Map<String, String> createRequest(final String code, final String filename) {
    return Map.of("code", code, "language", "java", "filename", filename);
  }

  @Test
  @DisplayName("should_detect_sql_injection_vulnerability_in_realistic_code")
  void should_detect_sql_injection_vulnerability_in_realistic_code() {
    final String vulnerableCode =
        """
        package com.example.ecommerce;

        import java.sql.*;

        public class UserController {
            private Connection dbConnection;

            public UserController() throws SQLException {
                this.dbConnection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/ecommerce",
                    "root",
                    "password123"
                );
            }

            public String getUserProfile(String userId) throws SQLException {
                Statement stmt = dbConnection.createStatement();
                String query = "SELECT * FROM users WHERE user_id = '" + userId + "'";
                ResultSet rs = stmt.executeQuery(query);

                StringBuilder profile = new StringBuilder();
                while (rs.next()) {
                    profile.append("Name: ").append(rs.getString("name"));
                    profile.append("Email: ").append(rs.getString("email"));
                }

                return profile.toString();
            }

            public void updateUserEmail(String userId, String newEmail) throws SQLException {
                Statement stmt = dbConnection.createStatement();
                String updateQuery = "UPDATE users SET email = '" + newEmail +
                                   "' WHERE user_id = '" + userId + "'";
                stmt.executeUpdate(updateQuery);
            }
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(createRequest(vulnerableCode, "UserController.java"))
        .when()
        .post("/api/security/analyze")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("tool", containsString("SpotBugs"))
        .body("findings", not(empty()))
        .body("findings.size()", greaterThan(0));
  }

  @Test
  @DisplayName("should_detect_hardcoded_credentials_and_secrets")
  void should_detect_hardcoded_credentials_and_secrets() {
    final String vulnerableCode =
        """
        package com.example.payment;

        import javax.crypto.Cipher;
        import javax.crypto.spec.SecretKeySpec;

        public class PaymentGateway {
            private static final byte[] CRYPTO_KEY = "hardcodedkey1234".getBytes();

            public byte[] encrypt(String data) throws Exception {
                SecretKeySpec keySpec = new SecretKeySpec(CRYPTO_KEY, "AES");
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                return cipher.doFinal(data.getBytes());
            }
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(createRequest(vulnerableCode, "PaymentGateway.java"))
        .when()
        .post("/api/security/analyze")
        .then()
        .statusCode(200)
        .body("findings", not(empty()))
        .body("findings.size()", greaterThan(0));
  }

  @Test
  @DisplayName("should_detect_weak_cryptography_and_insecure_random")
  void should_detect_weak_cryptography_and_insecure_random() {
    final String vulnerableCode =
        """
        package com.example.auth;

        import javax.crypto.*;
        import javax.crypto.spec.SecretKeySpec;
        import java.security.*;
        import java.util.Random;

        public class AuthenticationService {
            private static final byte[] ENCRYPTION_KEY = "hardcodedkey123".getBytes();

            public String generateSessionToken() {
                Random random = new Random();
                StringBuilder token = new StringBuilder();
                for (int i = 0; i < 32; i++) {
                    token.append((char) (random.nextInt(26) + 'a'));
                }
                return token.toString();
            }

            public byte[] encryptPassword(String password) throws Exception {
                KeyGenerator keyGen = KeyGenerator.getInstance("DES");
                SecretKey secretKey = keyGen.generateKey();

                Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);

                return cipher.doFinal(password.getBytes());
            }

            public String hashPassword(String password) throws Exception {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hash = md.digest(password.getBytes());
                return bytesToHex(hash);
            }

            private String bytesToHex(byte[] bytes) {
                StringBuilder result = new StringBuilder();
                for (byte b : bytes) {
                    result.append(String.format("%02x", b));
                }
                return result.toString();
            }

            public byte[] encryptWithStaticKey(String data) throws Exception {
                SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY, "AES");
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                return cipher.doFinal(data.getBytes());
            }
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(createRequest(vulnerableCode, "AuthenticationService.java"))
        .when()
        .post("/api/security/analyze")
        .then()
        .statusCode(200)
        .body("findings", not(empty()))
        .body("findings.size()", greaterThan(0));
  }

  @Test
  @DisplayName("should_detect_vulnerable_dependencies_log4shell")
  void should_detect_vulnerable_dependencies_log4shell() {
    final String vulnerableCode =
        """
        package com.example.logging;

        import org.apache.logging.log4j.LogManager;
        import org.apache.logging.log4j.Logger;

        public class ApplicationLogger {
            private static final Logger logger = LogManager.getLogger(ApplicationLogger.class);

            public void logUserInput(String userInput) {
                logger.info("User input received: {}", userInput);
            }

            public void logError(Exception e) {
                logger.error("Error occurred", e);
            }

            public void logDebug(String message) {
                logger.debug(message);
            }
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(createRequest(vulnerableCode, "ApplicationLogger.java"))
        .when()
        .post("/api/security/analyze")
        .then()
        .statusCode(200)
        .body("findings", not(empty()))
        .body(
            "findings.type",
            hasItem(
                anyOf(
                    containsString("CVE-2021-44228"),
                    containsString("log4j"),
                    containsString("LOG4SHELL"))));
  }

  @Test
  @DisplayName("should_detect_path_traversal_vulnerability")
  void should_detect_path_traversal_vulnerability() {
    final String vulnerableCode =
        """
        package com.example.filemanager;

        import java.io.*;

        public class FileDownloadController {
            private static final String BASE_PATH = "/var/www/uploads/";

            public void downloadFile(String filename) throws IOException {
                File file = new File(BASE_PATH + filename);

                if (file.exists()) {
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        System.out.write(buffer, 0, bytesRead);
                    }
                    fis.close();
                }
            }
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(createRequest(vulnerableCode, "FileDownloadController.java"))
        .when()
        .post("/api/security/analyze")
        .then()
        .statusCode(200)
        .body("findings.size()", greaterThan(0));
  }

  @Test
  @DisplayName("should_detect_xxe_vulnerability")
  void should_detect_xxe_vulnerability() {
    final String vulnerableCode =
        """
        package com.example.xml;

        import javax.xml.parsers.*;
        import org.w3c.dom.*;
        import java.io.*;

        public class XmlParser {
            public Document parseXml(InputStream xmlInput) throws Exception {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                return builder.parse(xmlInput);
            }

            public void processXmlFile(String filename) throws Exception {
                File xmlFile = new File(filename);
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(xmlFile);
                doc.getDocumentElement().normalize();
            }
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(createRequest(vulnerableCode, "XmlParser.java"))
        .when()
        .post("/api/security/analyze")
        .then()
        .statusCode(200)
        .body("findings", not(empty()))
        .body("findings.size()", greaterThan(0));
  }

  @Test
  @DisplayName("should_return_comprehensive_analysis_for_multi_vulnerability_code")
  void should_return_comprehensive_analysis_for_multi_vulnerability_code() {
    final String complexVulnerableCode =
        """
        package com.example.enterprise;

        import java.sql.*;
        import javax.crypto.*;
        import javax.crypto.spec.SecretKeySpec;
        import org.apache.logging.log4j.LogManager;
        import org.apache.logging.log4j.Logger;
        import java.util.Random;

        public class EnterpriseApplication {
            private static final Logger logger = LogManager.getLogger(EnterpriseApplication.class);
            private static final String DB_PASSWORD = "prod_password_123";
            private static final String API_KEY = "sk_live_AbCdEfGhIjKlMnOpQrStUvWx";
            private static final byte[] ENCRYPTION_KEY = "secretkey1234567".getBytes();

            private Connection connection;

            public EnterpriseApplication() throws SQLException {
                this.connection = DriverManager.getConnection(
                    "jdbc:mysql://prod-db.example.com:3306/maindb",
                    "admin",
                    DB_PASSWORD
                );
            }

            public String authenticateUser(String username, String password) throws Exception {
                Statement stmt = connection.createStatement();
                String query = "SELECT * FROM users WHERE username = '" + username +
                             "' AND password = '" + password + "'";
                ResultSet rs = stmt.executeQuery(query);

                if (rs.next()) {
                    return generateToken();
                }
                return null;
            }

            private String generateToken() {
                Random random = new Random(System.currentTimeMillis());
                StringBuilder token = new StringBuilder();
                for (int i = 0; i < 32; i++) {
                    token.append((char) (random.nextInt(26) + 'a'));
                }
                return token.toString();
            }

            public byte[] encryptData(String data) throws Exception {
                SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY, "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                return cipher.doFinal(data.getBytes());
            }

            public void logActivity(String userInput) {
                logger.info("User activity: " + userInput);
            }
        }
        """;

    final var response =
        given()
            .contentType(ContentType.JSON)
            .body(createRequest(complexVulnerableCode, "EnterpriseApplication.java"))
            .when()
            .post("/api/security/analyze")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("findings", not(empty()))
            .body("findings.size()", greaterThan(3))
            .body("tool", containsString("SpotBugs"))
            .body("toolVersion", notNullValue())
            .body("analysisTimeMs", greaterThan(0))
            .extract()
            .response();

    final var findings = response.jsonPath().getList("findings");
    assertThat(findings).hasSizeGreaterThan(3);

    final var findingTypes = response.jsonPath().getList("findings.type", String.class);
    assertThat(findingTypes).anyMatch(type -> type.contains("SQL") || type.contains("INJECTION"));
  }

  @Test
  @DisplayName("should_handle_secure_code_without_false_positives")
  void should_handle_secure_code_without_false_positives() {
    final String secureCode =
        """
        package com.example.secure;

        import java.sql.*;
        import javax.crypto.*;
        import javax.crypto.spec.*;
        import java.security.SecureRandom;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        public class SecureApplication {
            private static final Logger logger = LoggerFactory.getLogger(SecureApplication.class);
            private final SecureRandom secureRandom;

            public SecureApplication() {
                this.secureRandom = new SecureRandom();
            }

            public String authenticateUser(String username, String password, Connection conn)
                throws SQLException {
                String query = "SELECT id FROM users WHERE username = ? AND password_hash = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    stmt.setString(2, hashPassword(password));

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return generateSecureToken();
                        }
                    }
                }
                return null;
            }

            private String generateSecureToken() {
                byte[] tokenBytes = new byte[32];
                secureRandom.nextBytes(tokenBytes);
                return bytesToHex(tokenBytes);
            }

            private String hashPassword(String password) {
                return "hashed_" + password;
            }

            private String bytesToHex(byte[] bytes) {
                StringBuilder result = new StringBuilder();
                for (byte b : bytes) {
                    result.append(String.format("%02x", b));
                }
                return result.toString();
            }

            public void logActivity(String userId) {
                logger.info("User activity logged for user: {}", userId);
            }
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(createRequest(secureCode, "SecureApplication.java"))
        .when()
        .post("/api/security/analyze")
        .then()
        .statusCode(200)
        .body("findings", empty());
  }
}
