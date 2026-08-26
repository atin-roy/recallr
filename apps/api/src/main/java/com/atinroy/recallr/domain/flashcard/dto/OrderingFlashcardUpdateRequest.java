package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderingFlashcardUpdateRequest extends FlashcardUpdateRequest {
    private List<String> items;
}
