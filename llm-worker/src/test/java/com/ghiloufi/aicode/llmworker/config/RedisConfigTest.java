package com.ghiloufi.aicode.llmworker.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RedisConfig Tests")
final class RedisConfigTest {

  @Nested
  @DisplayName("ObjectMapper Bean")
  final class ObjectMapperBean {

    @Test
    @DisplayName("should_create_object_mapper")
    void should_create_object_mapper() {
      final RedisConfig config = new RedisConfig();

      final ObjectMapper mapper = config.objectMapper();

      assertThat(mapper).isNotNull();
    }

    @Test
    @DisplayName("should_register_java_time_module")
    void should_register_java_time_module() throws Exception {
      final RedisConfig config = new RedisConfig();
      final ObjectMapper mapper = config.objectMapper();

      final Instant now = Instant.parse("2024-01-15T10:30:00Z");
      final String json = mapper.writeValueAsString(new TimeHolder(now));

      assertThat(json).contains("timestamp");
    }

    @Test
    @DisplayName("should_deserialize_java_time")
    void should_deserialize_java_time() throws Exception {
      final RedisConfig config = new RedisConfig();
      final ObjectMapper mapper = config.objectMapper();

      final String json = "{\"timestamp\":1718894700.000000000}";
      final TimeHolder holder = mapper.readValue(json, TimeHolder.class);

      assertThat(holder.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("should_serialize_and_deserialize_instant")
    void should_serialize_and_deserialize_instant() throws Exception {
      final RedisConfig config = new RedisConfig();
      final ObjectMapper mapper = config.objectMapper();

      final Instant original = Instant.now();
      final TimeHolder holder = new TimeHolder(original);

      final String json = mapper.writeValueAsString(holder);
      final TimeHolder deserialized = mapper.readValue(json, TimeHolder.class);

      assertThat(deserialized.timestamp()).isEqualTo(original);
    }

    @Test
    @DisplayName("should_handle_null_timestamp")
    void should_handle_null_timestamp() throws Exception {
      final RedisConfig config = new RedisConfig();
      final ObjectMapper mapper = config.objectMapper();

      final TimeHolder holder = new TimeHolder(null);
      final String json = mapper.writeValueAsString(holder);

      assertThat(json).contains("null");
    }
  }

  @Nested
  @DisplayName("Configuration Instantiation")
  final class ConfigurationInstantiation {

    @Test
    @DisplayName("should_create_redis_config")
    void should_create_redis_config() {
      final RedisConfig config = new RedisConfig();

      assertThat(config).isNotNull();
    }
  }

  private record TimeHolder(Instant timestamp) {}
}
