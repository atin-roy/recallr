package com.atinroy.recallr.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        String description
) {}
