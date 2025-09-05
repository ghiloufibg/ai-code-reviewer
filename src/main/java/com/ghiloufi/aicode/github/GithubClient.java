
package com.ghiloufi.aicode.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class GithubClient {
    private final String repo;
    private final String token;
    private final ObjectMapper om = new ObjectMapper();
    private final CloseableHttpClient http = HttpClients.createDefault();

    public GithubClient(String repo, String token) {
        this.repo = repo;
        this.token = token;
    }

    public String fetchPrUnifiedDiff(int pr, int context) {
        try {
            String url = String.format("https://api.github.com/repos/%s/pulls/%d", repo, pr);
            HttpGet get = new HttpGet(url);
            get.addHeader("Accept", "application/vnd.github.v3.diff");
            if (token != null && !token.isBlank()) get.addHeader("Authorization", "Bearer " + token);
            var resp = http.execute(get);
            String body = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
            return body;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void postIssueComment(int pr, String body) {
        try {
            String url = String.format("https://api.github.com/repos/%s/issues/%d/comments", repo, pr);
            HttpPost post = new HttpPost(url);
            post.addHeader("Accept", "application/vnd.github+json");
            if (token != null && !token.isBlank()) post.addHeader("Authorization", "Bearer " + token);
            post.setEntity(new StringEntity(om.writeValueAsString(Map.of("body", body)), ContentType.APPLICATION_JSON));
            http.execute(post).close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createReview(int pr, List<ReviewComment> comments) {
        try {
            String url = String.format("https://api.github.com/repos/%s/pulls/%d/reviews", repo, pr);
            HttpPost post = new HttpPost(url);
            post.addHeader("Accept", "application/vnd.github+json");
            if (token != null && !token.isBlank()) post.addHeader("Authorization", "Bearer " + token);
            java.util.List<java.util.Map<String, Object>> arr = new java.util.ArrayList<>();
            for (var c : comments) {
                arr.add(Map.of("path", c.path(), "position", c.position(), "body", c.body()));
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "COMMENT");
            payload.put("comments", arr);
            post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(payload), ContentType.APPLICATION_JSON));
            http.execute(post).close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public record ReviewComment(String path, int position, String body) {
    }
}
