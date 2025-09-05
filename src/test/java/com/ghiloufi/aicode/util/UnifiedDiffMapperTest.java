package com.ghiloufi.aicode.util;

import com.ghiloufi.aicode.domain.UnifiedDiff;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UnifiedDiffMapperTest {
  @Test
  void mapPosition() {
    String diff =
"""
--- a/src/Foo.java
+++ b/src/Foo.java
@@ -1,1 +1,2 @@
- x
+ a
+ b
""";
    UnifiedDiff ud = new UnifiedDiffParser().parse(diff);
    UnifiedDiffMapper m = new UnifiedDiffMapper(ud);
    int pos = m.positionFor("src/Foo.java", 2);
    assertTrue(pos > 0);
  }
}
