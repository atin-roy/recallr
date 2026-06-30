package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MCQFlashcardRequest extends FlashcardRequest {
    private List<String> options;
    private int correctOptionIndex;
    private String explanation;
}
