package com.atinroy.recallr.domain.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BasicFlashcardResponse.class, name = "BASIC"),
    @JsonSubTypes.Type(value = MCQFlashcardResponse.class, name = "MCQ")
})
@Getter
@Setter
public abstract class FlashcardResponse {
    private UUID id;
    private UUID deckId;
    private String question;
    private Instant createdAt;
    private Instant updatedAt;
}
