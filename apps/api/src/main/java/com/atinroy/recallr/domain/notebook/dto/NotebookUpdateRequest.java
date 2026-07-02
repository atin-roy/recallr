package com.atinroy.recallr.domain.notebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NotebookUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @Pattern(regexp = "lavender|teal|yellow|mint|coral|sky|rose|peach") String color
) {}
