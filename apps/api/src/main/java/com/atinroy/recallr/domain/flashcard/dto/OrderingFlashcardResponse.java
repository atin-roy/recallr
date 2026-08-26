package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderingFlashcardResponse extends FlashcardResponse {
    private List<String> items;
}
