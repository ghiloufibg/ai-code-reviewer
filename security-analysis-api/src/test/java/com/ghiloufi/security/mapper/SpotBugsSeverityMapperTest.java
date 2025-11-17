package com.ghiloufi.security.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpotBugsSeverityMapperTest {

  @Test
  void should_map_priority_1_to_high() {
    final String severity = SpotBugsSeverityMapper.mapPriority(1);
    assertThat(severity).isEqualTo("HIGH");
  }

  @Test
  void should_map_priority_2_to_medium() {
    final String severity = SpotBugsSeverityMapper.mapPriority(2);
    assertThat(severity).isEqualTo("MEDIUM");
  }

  @Test
  void should_map_priority_3_to_low() {
    final String severity = SpotBugsSeverityMapper.mapPriority(3);
    assertThat(severity).isEqualTo("LOW");
  }

  @Test
  void should_map_priority_4_to_low() {
    final String severity = SpotBugsSeverityMapper.mapPriority(4);
    assertThat(severity).isEqualTo("LOW");
  }

  @Test
  void should_map_negative_priority_to_low() {
    final String severity = SpotBugsSeverityMapper.mapPriority(-1);
    assertThat(severity).isEqualTo("LOW");
  }
}
