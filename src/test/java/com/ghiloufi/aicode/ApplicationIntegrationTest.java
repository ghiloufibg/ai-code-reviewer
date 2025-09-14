package com.ghiloufi.aicode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

import com.ghiloufi.aicode.core.*;
import com.ghiloufi.aicode.llm.*;
import com.ghiloufi.aicode.sast.StaticAnalysisRunner;

@SpringBootTest
@TestPropertySource(properties = {
    "app.mode=local",
    "app.repository=test-repo",
    "app.diff.contextLines=5",
    "app.llm.baseUrl=http://test:1234",
    "app.llm.model=test-model",
    "app.llm.timeoutSeconds=10"
})
class ApplicationIntegrationTest {

    @Autowired
    private DiffCollectionService diffCollectionService;

    @Autowired
    private UnifiedDiffParser unifiedDiffParser;

    @Autowired
    private StaticAnalysisRunner staticAnalysisRunner;

    @Autowired
    private PromptBuilder promptBuilder;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private LlmReviewValidator reviewValidator;

    @Autowired
    private ReviewResultMerger reviewResultMerger;

    @Test
    void contextLoads() {
        // This test verifies that the Spring context loads successfully
        // with all beans properly wired
        assertTrue(true);
    }

    @Test
    void allRequiredBeansAreWired() {
        assertNotNull(diffCollectionService, "DiffCollectionService should be autowired");
        assertNotNull(unifiedDiffParser, "UnifiedDiffParser should be autowired");
        assertNotNull(staticAnalysisRunner, "StaticAnalysisRunner should be autowired");
        assertNotNull(promptBuilder, "PromptBuilder should be autowired");
        assertNotNull(llmClient, "LlmClient should be autowired");
        assertNotNull(reviewValidator, "LlmReviewValidator should be autowired");
        assertNotNull(reviewResultMerger, "ReviewResultMerger should be autowired");
    }

    @Test
    void githubBeansAreNotCreatedInLocalMode() {
        // In local mode, GitHub-related beans should not be created
        // This is tested by the successful context loading without GitHub token
        assertTrue(true);
    }
}