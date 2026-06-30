package com.atinroy.recallr.domain.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BasicFlashcardUpdateRequest.class, name = "BASIC"),
    @JsonSubTypes.Type(value = MCQFlashcardUpdateRequest.class, name = "MCQ")
})
@Getter
@Setter
public abstract class FlashcardUpdateRequest {
    private String question;
}
