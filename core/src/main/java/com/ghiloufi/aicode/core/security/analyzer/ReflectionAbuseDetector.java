package com.ghiloufi.aicode.core.security.analyzer;

import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class ReflectionAbuseDetector extends BaseSecurityVisitor {

  private static final List<String> DANGEROUS_REFLECTION_METHODS =
      List.of(
          "forName",
          "newInstance",
          "invoke",
          "getDeclaredMethod",
          "getMethod",
          "getDeclaredConstructor",
          "getConstructor");

  private static final List<String> REFLECTION_CLASSES = List.of("Class", "Method", "Constructor");

  @Override
  protected void analyzeMethodCall(
      final MethodCallExpr methodCall, final List<SecurityIssue> issues) {
    final String methodName = methodCall.getNameAsString();

    if ("forName".equals(methodName)) {
      final String scopeString = methodCall.getScope().map(Object::toString).orElse("");
      if (scopeString.contains("Class")) {
        if (hasStringConcatenation(methodCall) || hasDynamicInput(methodCall)) {
          issues.add(
              SecurityIssue.builder()
                  .severity(com.ghiloufi.aicode.core.security.model.Severity.CRITICAL)
                  .category("REFLECTION_ABUSE")
                  .description(
                      String.format(
                          "Class.forName() with dynamic input detected at line %d. "
                              + "This can lead to arbitrary code execution if user input is involved.",
                          methodCall.getBegin().map(pos -> pos.line).orElse(-1)))
                  .recommendation(
                      "Avoid using Class.forName() with user-controlled input. "
                          + "Use allowlists for permitted classes or design patterns that don't require reflection.")
                  .build());
        } else {
          issues.add(
              SecurityIssue.builder()
                  .severity(com.ghiloufi.aicode.core.security.model.Severity.HIGH)
                  .category("REFLECTION_USAGE")
                  .description(
                      String.format(
                          "Class.forName() detected at line %d. "
                              + "Verify that no user input influences the loaded class.",
                          methodCall.getBegin().map(pos -> pos.line).orElse(-1)))
                  .recommendation(
                      "Ensure class names are from trusted sources. "
                          + "Consider using dependency injection instead of Class.forName().")
                  .build());
        }
        return;
      }
    }

    if (!DANGEROUS_REFLECTION_METHODS.contains(methodName)) {
      if ("newInstance".equals(methodName) || "invoke".equals(methodName)) {
        final String scopeString = methodCall.getScope().map(Object::toString).orElse("");
        if (scopeString.contains("forName")
            || scopeString.contains("getDeclaredConstructor")
            || scopeString.contains("getConstructor")
            || scopeString.contains("getDeclaredMethod")
            || scopeString.contains("getMethod")) {
          issues.add(
              SecurityIssue.builder()
                  .severity(com.ghiloufi.aicode.core.security.model.Severity.HIGH)
                  .category("REFLECTION_USAGE")
                  .description(
                      String.format(
                          "Reflection API call '%s()' detected at line %d. "
                              + "Verify that no user input influences the reflected class/method.",
                          methodName, methodCall.getBegin().map(pos -> pos.line).orElse(-1)))
                  .recommendation(
                      "Ensure reflection targets are from trusted sources. "
                          + "Consider using interfaces or dependency injection instead of reflection.")
                  .build());
        }
      }
      return;
    }

    if (isReflectionCall(methodCall)) {
      if (hasStringConcatenation(methodCall) || hasDynamicInput(methodCall)) {
        issues.add(
            SecurityIssue.builder()
                .severity(com.ghiloufi.aicode.core.security.model.Severity.CRITICAL)
                .category("REFLECTION_ABUSE")
                .description(
                    String.format(
                        "Reflection API call '%s()' with dynamic input detected at line %d. "
                            + "This can lead to arbitrary code execution if user input is involved.",
                        methodName, methodCall.getBegin().map(pos -> pos.line).orElse(-1)))
                .recommendation(
                    "Avoid using reflection with user-controlled input. "
                        + "Use allowlists for permitted classes/methods or design patterns that don't require reflection.")
                .build());
      } else {
        issues.add(
            SecurityIssue.builder()
                .severity(com.ghiloufi.aicode.core.security.model.Severity.HIGH)
                .category("REFLECTION_USAGE")
                .description(
                    String.format(
                        "Reflection API call '%s()' detected at line %d. "
                            + "Verify that no user input influences the reflected class/method.",
                        methodName, methodCall.getBegin().map(pos -> pos.line).orElse(-1)))
                .recommendation(
                    "Ensure reflection targets are from trusted sources. "
                        + "Consider using interfaces or dependency injection instead of reflection.")
                .build());
      }
    }
  }

  private boolean isReflectionCall(final MethodCallExpr methodCall) {
    if (!methodCall.getScope().isPresent()) {
      return false;
    }

    final String scopeString = methodCall.getScope().get().toString();
    final String scopeWithoutClass = scopeString.replace(".class", "");

    if (REFLECTION_CLASSES.stream().anyMatch(scopeWithoutClass::contains)) {
      return true;
    }

    if (scopeString.contains("forName")
        || scopeString.contains("getDeclaredConstructor")
        || scopeString.contains("getConstructor")
        || scopeString.contains("getDeclaredMethod")
        || scopeString.contains("getMethod")) {
      return true;
    }

    final String methodName = methodCall.getNameAsString();
    return "invoke".equals(methodName) || "newInstance".equals(methodName);
  }

  private boolean hasDynamicInput(final MethodCallExpr methodCall) {
    return methodCall.getArguments().stream()
        .anyMatch(
            arg -> {
              final String argStr = arg.toString();
              return argStr.contains("request.getParameter")
                  || argStr.contains("scanner.next")
                  || argStr.contains("BufferedReader")
                  || argStr.contains("System.getProperty")
                  || argStr.contains("readLine");
            });
  }
}
