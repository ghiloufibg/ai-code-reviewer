package com.ghiloufi.aicode.core.security.analyzer;

import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class PathTraversalDetector extends BaseSecurityVisitor {

  private static final List<String> FILE_OPERATIONS =
      List.of("File", "Path", "Paths", "FileInputStream", "FileOutputStream", "RandomAccessFile");

  private static final List<String> DANGEROUS_PATH_METHODS =
      List.of("get", "of", "resolve", "resolveSibling");

  @Override
  protected void analyzeMethodCall(
      final MethodCallExpr methodCall, final List<SecurityIssue> issues) {
    final String methodName = methodCall.getNameAsString();

    if (DANGEROUS_PATH_METHODS.contains(methodName)) {
      if (hasStringConcatenation(methodCall) || hasPathConcatenation(methodCall)) {
        issues.add(
            SecurityIssue.builder()
                .severity(com.ghiloufi.aicode.core.security.model.Severity.HIGH)
                .category("PATH_TRAVERSAL")
                .description(
                    String.format(
                        "Path.%s() with string concatenation detected at line %d. "
                            + "This can lead to directory traversal attacks (e.g., ../../../etc/passwd).",
                        methodName, methodCall.getBegin().map(pos -> pos.line).orElse(-1)))
                .recommendation(
                    "Use Path.of() or Paths.get() without concatenation. "
                        + "Validate paths with normalize() and check startsWith() against allowed base paths.")
                .build());
      }
    }
  }

  @Override
  public void visit(final ObjectCreationExpr objectCreation, final List<SecurityIssue> issues) {
    final String typeName = objectCreation.getTypeAsString();

    if (FILE_OPERATIONS.stream().anyMatch(typeName::equals)) {
      if (hasStringConcatenationInArgs(objectCreation)) {
        issues.add(
            SecurityIssue.builder()
                .severity(com.ghiloufi.aicode.core.security.model.Severity.HIGH)
                .category("PATH_TRAVERSAL")
                .description(
                    String.format(
                        "File operation with string concatenation detected at line %d. "
                            + "Use Path.of().normalize() to prevent directory traversal.",
                        objectCreation.getBegin().map(pos -> pos.line).orElse(-1)))
                .recommendation(
                    "Use Path.of() without concatenation, call normalize(), and validate against base directory.")
                .build());
      } else if (hasUserInputInPath(objectCreation)) {
        issues.add(
            SecurityIssue.builder()
                .severity(com.ghiloufi.aicode.core.security.model.Severity.MEDIUM)
                .category("PATH_TRAVERSAL_RISK")
                .description(
                    String.format(
                        "File operation with potential user input detected at line %d. "
                            + "Validate and sanitize file paths before use.",
                        objectCreation.getBegin().map(pos -> pos.line).orElse(-1)))
                .recommendation(
                    "Sanitize file paths: use normalize(), check for '..' sequences, and validate against allowed directories.")
                .build());
      }
    }

    super.visit(objectCreation, issues);
  }

  private boolean hasPathConcatenation(final MethodCallExpr methodCall) {
    return methodCall.getArguments().stream()
        .anyMatch(
            arg -> {
              final String argStr = arg.toString();
              return argStr.contains("File.separator")
                  || argStr.contains("\"/\"")
                  || argStr.contains("\"\\\\\"");
            });
  }

  private boolean hasStringConcatenationInArgs(final ObjectCreationExpr objectCreation) {
    return objectCreation.getArguments().stream()
        .anyMatch(arg -> arg.toString().contains("+") || arg.toString().contains("concat"));
  }

  private boolean hasUserInputInPath(final ObjectCreationExpr objectCreation) {
    return objectCreation.getArguments().stream()
        .anyMatch(
            arg -> {
              final String argStr = arg.toString();
              return argStr.contains("request.")
                  || argStr.contains("getParameter")
                  || argStr.contains("scanner.")
                  || argStr.contains("readLine")
                  || argStr.contains("System.getProperty");
            });
  }
}
