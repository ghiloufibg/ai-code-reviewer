package com.ghiloufi.aicode.core.security.analyzer;

import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class CodeInjectionDetector extends BaseSecurityVisitor {

  private static final List<String> SCRIPT_ENGINE_METHODS = List.of("eval", "compile");

  private static final List<String> DANGEROUS_TYPES =
      List.of("ScriptEngine", "ScriptEngineManager", "Compiler", "ToolProvider");

  @Override
  protected void analyzeMethodCall(
      final MethodCallExpr methodCall, final List<SecurityIssue> issues) {
    final String methodName = methodCall.getNameAsString();
    final String scopeString = methodCall.getScope().map(Object::toString).orElse("");

    if (scopeString.contains("ToolProvider")) {
      issues.add(
          SecurityIssue.builder()
              .severity(com.ghiloufi.aicode.core.security.model.Severity.HIGH)
              .category("DYNAMIC_CODE_EXECUTION")
              .description(
                  String.format(
                      "Dynamic code execution API 'ToolProvider' detected at line %d. "
                          + "Ensure proper input validation and sandboxing.",
                      methodCall.getBegin().map(pos -> pos.line).orElse(-1)))
              .recommendation(
                  "Avoid ToolProvider in production. If required, run in isolated sandbox with strict input validation.")
              .build());
      return;
    }

    if (SCRIPT_ENGINE_METHODS.contains(methodName)) {
      if (hasStringConcatenation(methodCall) || hasDynamicScriptInput(methodCall)) {
        issues.add(
            SecurityIssue.builder()
                .severity(com.ghiloufi.aicode.core.security.model.Severity.CRITICAL)
                .category("CODE_INJECTION")
                .description(
                    String.format(
                        "ScriptEngine.%s() with dynamic input detected at line %d. "
                            + "This allows arbitrary code execution if user input is involved.",
                        methodName, methodCall.getBegin().map(pos -> pos.line).orElse(-1)))
                .recommendation(
                    "Never execute user-provided scripts. "
                        + "Use a safe DSL or configuration format (JSON, YAML) instead of script engines.")
                .build());
      } else {
        issues.add(
            SecurityIssue.builder()
                .severity(com.ghiloufi.aicode.core.security.model.Severity.HIGH)
                .category("DYNAMIC_CODE_EXECUTION")
                .description(
                    String.format(
                        "ScriptEngine.%s() detected at line %d. "
                            + "Verify that no user input is passed to this method.",
                        methodName, methodCall.getBegin().map(pos -> pos.line).orElse(-1)))
                .recommendation(
                    "Ensure scripts are from trusted sources only. Consider using safer alternatives like expression libraries.")
                .build());
      }
    }
  }

  @Override
  public void visit(final ObjectCreationExpr objectCreation, final List<SecurityIssue> issues) {
    final String typeName = objectCreation.getTypeAsString();

    if (DANGEROUS_TYPES.stream().anyMatch(typeName::contains)) {
      issues.add(
          SecurityIssue.builder()
              .severity(com.ghiloufi.aicode.core.security.model.Severity.HIGH)
              .category("DYNAMIC_CODE_EXECUTION")
              .description(
                  String.format(
                      "Dynamic code execution API '%s' detected at line %d. "
                          + "Ensure proper input validation and sandboxing.",
                      typeName, objectCreation.getBegin().map(pos -> pos.line).orElse(-1)))
              .recommendation(
                  "Avoid dynamic code execution in production. Use safer alternatives like expression evaluators with limited scope.")
              .build());
    }

    super.visit(objectCreation, issues);
  }

  private boolean hasDynamicScriptInput(final MethodCallExpr methodCall) {
    return methodCall.getArguments().stream()
        .anyMatch(
            arg -> {
              final String argStr = arg.toString();
              return argStr.contains("request.")
                  || argStr.contains("scanner.")
                  || argStr.contains("readLine")
                  || argStr.contains("getParameter")
                  || argStr.contains("getProperty");
            });
  }
}
