package com.ghiloufi.aicode.core.infrastructure.adapter;

import java.util.List;

public record PublishResult(
    int inlineCommentsCreated,
    int fallbackCommentsInBody,
    List<String> discussionIds,
    List<PublishError> errors) {}
