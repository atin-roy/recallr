package com.atinroy.recallr.domain.mcq.dto;

import java.util.List;

public record MCQResponse(
        String id,
        String question,
        List<String> options,
        int correctOptionIndex,
        String explanation,
        String subjectId,
        String topicId
) {}
