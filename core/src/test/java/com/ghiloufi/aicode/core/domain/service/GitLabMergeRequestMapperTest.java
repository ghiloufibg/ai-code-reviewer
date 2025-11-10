package com.ghiloufi.aicode.core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ghiloufi.aicode.core.domain.model.MergeRequestSummary;
import java.time.Instant;
import java.util.Date;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.models.MergeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GitLabMergeRequestMapper")
final class GitLabMergeRequestMapperTest {

  private GitLabMergeRequestMapper mapper;

  @BeforeEach
  final void setUp() {
    mapper = new GitLabMergeRequestMapper();
  }

  @Nested
  @DisplayName("when mapping merge request to summary")
  final class ToMergeRequestSummary {

    @Test
    @DisplayName("should_map_all_fields_correctly")
    final void should_map_all_fields_correctly() {
      final MergeRequest mr = createFullMergeRequest();

      final MergeRequestSummary result = mapper.toMergeRequestSummary(mr);

      assertThat(result.iid()).isEqualTo(123);
      assertThat(result.title()).isEqualTo("Test MR");
      assertThat(result.description()).isEqualTo("Test description");
      assertThat(result.state()).isEqualTo("opened");
      assertThat(result.author()).isEqualTo("testuser");
      assertThat(result.sourceBranch()).isEqualTo("feature");
      assertThat(result.targetBranch()).isEqualTo("main");
      assertThat(result.createdAt()).isNotNull();
      assertThat(result.updatedAt()).isNotNull();
      assertThat(result.webUrl()).isEqualTo("https://gitlab.com/test/mr/123");
    }

    @Test
    @DisplayName("should_use_default_number_when_iid_is_null")
    final void should_use_default_number_when_iid_is_null() {
      final MergeRequest mr = createFullMergeRequest();
      mr.setIid(null);

      final MergeRequestSummary result = mapper.toMergeRequestSummary(mr);

      assertThat(result.iid()).isZero();
    }

    @Test
    @DisplayName("should_use_empty_description_when_null")
    final void should_use_empty_description_when_null() {
      final MergeRequest mr = createFullMergeRequest();
      mr.setDescription(null);

      final MergeRequestSummary result = mapper.toMergeRequestSummary(mr);

      assertThat(result.description()).isEmpty();
    }

    @Test
    @DisplayName("should_use_unknown_author_when_null")
    final void should_use_unknown_author_when_null() {
      final MergeRequest mr = createFullMergeRequest();
      mr.setAuthor(null);

      final MergeRequestSummary result = mapper.toMergeRequestSummary(mr);

      assertThat(result.author()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("should_handle_null_created_at")
    final void should_handle_null_created_at() {
      final MergeRequest mr = createFullMergeRequest();
      mr.setCreatedAt(null);

      final MergeRequestSummary result = mapper.toMergeRequestSummary(mr);

      assertThat(result.createdAt()).isNull();
    }

    @Test
    @DisplayName("should_handle_null_updated_at")
    final void should_handle_null_updated_at() {
      final MergeRequest mr = createFullMergeRequest();
      mr.setUpdatedAt(null);

      final MergeRequestSummary result = mapper.toMergeRequestSummary(mr);

      assertThat(result.updatedAt()).isNull();
    }

    @Test
    @DisplayName("should_handle_merge_request_with_all_nulls")
    final void should_handle_merge_request_with_all_nulls() {
      final MergeRequest mr = new MergeRequest();
      mr.setTitle("Minimal MR");
      mr.setState("opened");
      mr.setSourceBranch("feature");
      mr.setTargetBranch("main");
      mr.setWebUrl("https://gitlab.com/test/mr");

      final MergeRequestSummary result = mapper.toMergeRequestSummary(mr);

      assertThat(result.iid()).isZero();
      assertThat(result.description()).isEmpty();
      assertThat(result.author()).isEqualTo("unknown");
      assertThat(result.createdAt()).isNull();
      assertThat(result.updatedAt()).isNull();
    }

    private MergeRequest createFullMergeRequest() {
      final MergeRequest mr = new MergeRequest();
      mr.setIid(123L);
      mr.setTitle("Test MR");
      mr.setDescription("Test description");
      mr.setState("opened");

      final Author author = new Author();
      author.setUsername("testuser");
      mr.setAuthor(author);

      mr.setSourceBranch("feature");
      mr.setTargetBranch("main");
      mr.setCreatedAt(Date.from(Instant.parse("2024-01-01T10:00:00Z")));
      mr.setUpdatedAt(Date.from(Instant.parse("2024-01-02T10:00:00Z")));
      mr.setWebUrl("https://gitlab.com/test/mr/123");

      return mr;
    }
  }
}
