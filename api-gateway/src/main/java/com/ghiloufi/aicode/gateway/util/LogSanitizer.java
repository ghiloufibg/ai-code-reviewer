package com.ghiloufi.aicode.gateway.util;

import java.util.regex.Pattern;

public final class LogSanitizer {

  private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n]");

  private LogSanitizer() {}

  public static String sanitize(final String input) {
    if (input == null) {
      return null;
    }
    return CRLF_PATTERN.matcher(input).replaceAll(" ");
  }
}
