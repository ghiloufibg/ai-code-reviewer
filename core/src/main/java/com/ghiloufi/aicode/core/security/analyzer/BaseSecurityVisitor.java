package com.ghiloufi.aicode.core.security.analyzer;

import com.ghiloufi.aicode.core.security.model.SecurityIssue;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.List;

public abstract class BaseSecurityVisitor extends VoidVisitorAdapter<List<SecurityIssue>> {

  @Override
  public void visit(final MethodCallExpr methodCall, final List<SecurityIssue> issues) {
    analyzeMethodCall(methodCall, issues);
    super.visit(methodCall, issues);
  }

  protected abstract void analyzeMethodCall(MethodCallExpr methodCall, List<SecurityIssue> issues);

  protected boolean isMethodNamed(final MethodCallExpr methodCall, final String methodName) {
    return methodCall.getNameAsString().equals(methodName);
  }

  protected boolean hasStringConcatenation(final MethodCallExpr methodCall) {
    return methodCall.getArguments().stream()
        .anyMatch(arg -> arg.toString().contains("+") || arg.toString().contains("String.format"));
  }
}
