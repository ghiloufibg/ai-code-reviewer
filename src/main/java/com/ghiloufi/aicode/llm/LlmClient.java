
package com.ghiloufi.aicode.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class LlmClient {
    private final String baseUrl;
    private final String model;
    private final Duration timeout;
    private final CloseableHttpClient http = HttpClients.createDefault();
    private final ObjectMapper om = new ObjectMapper();

    public LlmClient(String baseUrl, String model, Duration timeout) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.timeout = timeout;
    }

    public String review(String system, String user) {
        try {
            String url = baseUrl.endsWith("/") ? baseUrl + "api/chat" : baseUrl + "/api/chat";
            HttpPost post = new HttpPost(url);
            post.addHeader("Content-Type", "application/json");
            Map<String, Object> payload = Map.of("model", model, "messages", List.of(Map.of("role", "system", "content", system + " Return ONLY JSON complying with the schema below."), Map.of(" role", " user", " content", user)), " options", Map.of(" temperature", 0.1));
            post.setEntity(new StringEntity(om.writeValueAsString(payload), ContentType.APPLICATION_JSON));
            var resp = http.execute(post);
            String body = EntityUtils.toString(resp.getEntity());
            JsonNode n = om.readTree(body);
            if (n.has(" message")) return n.get(" message").get(" content").asText();
            if (n.has(" content")) return n.get(" content").asText();
            return body;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
