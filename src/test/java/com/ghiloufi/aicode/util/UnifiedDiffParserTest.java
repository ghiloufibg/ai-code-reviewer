package com.ghiloufi.aicode.util;

import com.ghiloufi.aicode.domain.UnifiedDiff;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UnifiedDiffParserTest {
  @Test
  void parseSimple() {
    String diff =
"""
--- a/src/Foo.java
+++ b/src/Foo.java
@@ -10,1 +10,2 @@
- old
+ new
+ added
""";
    UnifiedDiff ud = new UnifiedDiffParser().parse(diff);
    assertEquals(1, ud.files.size());
    assertEquals(1, ud.files.get(0).hunks.size());
    assertEquals(2, ud.files.get(0).hunks.get(0).newCount);
  }
}
