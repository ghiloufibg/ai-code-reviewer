package com.ghiloufi.aicode.core.security.analyzer;

import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class CommandInjectionDetector extends BaseSecurityVisitor {

  private static final String PROCESS_BUILDER = "ProcessBuilder";

  @Override
  protected void analyzeMethodCall(
      final MethodCallExpr methodCall, final List<SecurityIssue> issues) {
    if (isRuntimeExec(methodCall)) {
      if (hasStringConcatenation(methodCall) || hasUserInput(methodCall)) {
        issues.add(
            SecurityIssue.builder()
                .severity(com.ghiloufi.aicode.core.security.model.Severity.CRITICAL)
                .category("COMMAND_INJECTION")
                .description(
                    String.format(
                        "Runtime.exec() with string concatenation or user input detected at line %d. "
                            + "This allows command injection if user input is involved.",
                        methodCall.getBegin().map(pos -> pos.line).orElse(-1)))
                .recommendation(
                    "Use ProcessBuilder with separate arguments instead of string concatenation. "
                        + "Validate and sanitize all user input before executing commands.")
                .build());
      } else {
        issues.add(
            SecurityIssue.builder()
                .severity(com.ghiloufi.aicode.core.security.model.Severity.HIGH)
                .category("COMMAND_EXECUTION")
                .description(
                    String.format(
                        "Runtime.exec() detected at line %d. "
                            + "Verify that no user input is passed to this method.",
                        methodCall.getBegin().map(pos -> pos.line).orElse(-1)))
                .recommendation(
                    "Ensure no user input influences command execution. Use allowlists for permitted commands.")
                .build());
      }
    }
  }

  @Override
  public void visit(final ObjectCreationExpr objectCreation, final List<SecurityIssue> issues) {
    if (isProcessBuilder(objectCreation)) {
      if (hasStringConcatenationInArgs(objectCreation)) {
        issues.add(
            SecurityIssue.builder()
                .severity(com.ghiloufi.aicode.core.security.model.Severity.CRITICAL)
                .category("COMMAND_INJECTION")
                .description(
                    String.format(
                        "ProcessBuilder with string concatenation detected at line %d. "
                            + "This allows command injection if user input is involved.",
                        objectCreation.getBegin().map(pos -> pos.line).orElse(-1)))
                .recommendation(
                    "Pass command arguments separately to ProcessBuilder constructor. "
                        + "Validate all user input before executing commands.")
                .build());
      } else {
        issues.add(
            SecurityIssue.builder()
                .severity(com.ghiloufi.aicode.core.security.model.Severity.MEDIUM)
                .category("COMMAND_EXECUTION")
                .description(
                    String.format(
                        "ProcessBuilder detected at line %d. "
                            + "Verify that no user input is passed to this constructor.",
                        objectCreation.getBegin().map(pos -> pos.line).orElse(-1)))
                .recommendation(
                    "Ensure command arguments are from trusted sources. Use allowlists for permitted commands.")
                .build());
      }
    }
    super.visit(objectCreation, issues);
  }

  private boolean isRuntimeExec(final MethodCallExpr methodCall) {
    if (!"exec".equals(methodCall.getNameAsString())) {
      return false;
    }

    return methodCall.getScope().isPresent()
        && methodCall.getScope().get().toString().contains("Runtime");
  }

  private boolean isProcessBuilder(final ObjectCreationExpr objectCreation) {
    return objectCreation.getTypeAsString().equals(PROCESS_BUILDER);
  }

  private boolean hasStringConcatenationInArgs(final ObjectCreationExpr objectCreation) {
    return objectCreation.getArguments().stream()
        .anyMatch(arg -> arg.toString().contains("+") || arg.toString().contains("String.format"));
  }

  private boolean hasUserInput(final MethodCallExpr methodCall) {
    return methodCall.getArguments().stream()
        .anyMatch(
            arg -> {
              final String argStr = arg.toString();
              return argStr.contains("request.getParameter")
                  || argStr.contains("request.getHeader")
                  || argStr.contains("scanner.next")
                  || argStr.contains("BufferedReader")
                  || argStr.contains("System.getProperty")
                  || argStr.contains("readLine");
            });
  }
}
