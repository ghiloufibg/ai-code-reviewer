package com.ghiloufi.aicode.llmworker.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WorkerProperties Tests")
final class WorkerPropertiesTest {

  @Nested
  @DisplayName("Property Access")
  final class PropertyAccess {

    @Test
    @DisplayName("should_return_consumer_group_when_provided")
    void should_return_consumer_group_when_provided() {
      final WorkerProperties props = new WorkerProperties("my-group", "worker-1", "stream", 10, 60);

      assertThat(props.getConsumerGroup()).isEqualTo("my-group");
    }

    @Test
    @DisplayName("should_return_null_consumer_group_when_null_provided")
    void should_return_null_consumer_group_when_null_provided() {
      final WorkerProperties props = new WorkerProperties(null, "worker-1", "stream", 10, 60);

      assertThat(props.getConsumerGroup()).isNull();
    }

    @Test
    @DisplayName("should_return_stream_key_when_provided")
    void should_return_stream_key_when_provided() {
      final WorkerProperties props =
          new WorkerProperties("group", "worker", "custom:stream", 10, 60);

      assertThat(props.getStreamKey()).isEqualTo("custom:stream");
    }

    @Test
    @DisplayName("should_return_null_stream_key_when_null_provided")
    void should_return_null_stream_key_when_null_provided() {
      final WorkerProperties props = new WorkerProperties("group", "worker-1", null, 10, 60);

      assertThat(props.getStreamKey()).isNull();
    }
  }

  @Nested
  @DisplayName("Consumer ID Generation")
  final class ConsumerIdGeneration {

    @Test
    @DisplayName("should_generate_consumer_id_when_null")
    void should_generate_consumer_id_when_null() {
      final WorkerProperties props = new WorkerProperties("group", null, "stream", 5, 30);

      assertThat(props.getConsumerId()).startsWith("worker-");
    }

    @Test
    @DisplayName("should_use_provided_consumer_id")
    void should_use_provided_consumer_id() {
      final WorkerProperties props = new WorkerProperties("group", "my-consumer", "stream", 5, 30);

      assertThat(props.getConsumerId()).isEqualTo("my-consumer");
    }

    @Test
    @DisplayName("should_handle_empty_consumer_id")
    void should_handle_empty_consumer_id() {
      final WorkerProperties props = new WorkerProperties("group", "", "stream", 10, 60);

      assertThat(props.getConsumerId()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Batch and Timeout Configuration")
  final class BatchAndTimeoutConfiguration {

    @Test
    @DisplayName("should_return_batch_size")
    void should_return_batch_size() {
      final WorkerProperties props = new WorkerProperties("group", "worker", "stream", 25, 60);

      assertThat(props.getBatchSize()).isEqualTo(25);
    }

    @Test
    @DisplayName("should_return_timeout_seconds")
    void should_return_timeout_seconds() {
      final WorkerProperties props = new WorkerProperties("group", "worker", "stream", 10, 180);

      assertThat(props.getTimeoutSeconds()).isEqualTo(180);
    }

    @Test
    @DisplayName("should_handle_zero_batch_size")
    void should_handle_zero_batch_size() {
      final WorkerProperties props = new WorkerProperties("group", "worker", "stream", 0, 60);

      assertThat(props.getBatchSize()).isZero();
    }

    @Test
    @DisplayName("should_handle_zero_timeout")
    void should_handle_zero_timeout() {
      final WorkerProperties props = new WorkerProperties("group", "worker", "stream", 10, 0);

      assertThat(props.getTimeoutSeconds()).isZero();
    }

    @Test
    @DisplayName("should_handle_large_batch_size")
    void should_handle_large_batch_size() {
      final WorkerProperties props = new WorkerProperties("group", "worker", "stream", 1000, 60);

      assertThat(props.getBatchSize()).isEqualTo(1000);
    }
  }
}
