
package com.ghiloufi.aicode.sast;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;

public class StaticAnalysisRunner {
    public Map<String, Object> runAndCollect() {
        try {
            Map<String, Object> out = new HashMap<>();
            Path t = Path.of("target");
            out.put("checkstyle", readIfExists(t.resolve("checkstyle-result.xml")));
            out.put("pmd", readIfExists(t.resolve("pmd.xml")));
            out.put("spotbugs", readIfExists(t.resolve("spotbugs.xml")));
            out.put("semgrep", readIfExists(t.resolve("semgrep.json")));
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object readIfExists(Path p) {
        try {
            if (Files.exists(p)) {
                String s = Files.readString(p);
                if (p.toString().endsWith(".json")) return new ObjectMapper().readValue(s, Object.class);
                return s.substring(0, min(s.length(), 200000));
            }
            return "";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int min(int a, int b) {
        return a < b ? a : b;
    }
}
