package com.atinroy.recallr.domain.topic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TopicRequest(
        @NotBlank @Size(max = 200) String name,
        String description
) {}
