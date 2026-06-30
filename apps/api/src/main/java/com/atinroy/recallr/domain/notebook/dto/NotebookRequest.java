package com.atinroy.recallr.domain.notebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotebookRequest(
        @NotBlank @Size(max = 200) String name,
        String description
) {}
