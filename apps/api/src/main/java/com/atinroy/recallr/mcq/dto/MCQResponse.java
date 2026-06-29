package com.atinroy.recallr.mcq.dto;

import java.util.List;

public record MCQResponse(
        String id,
        String question,
        List<String> options,
        int correctOptionIndex,
        String explanation
) {
}
