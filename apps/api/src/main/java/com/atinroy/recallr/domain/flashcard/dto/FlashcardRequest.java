package com.atinroy.recallr.domain.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BasicFlashcardRequest.class, name = "BASIC"),
    @JsonSubTypes.Type(value = MCQFlashcardRequest.class, name = "MCQ")
})
@Getter
@Setter
public abstract class FlashcardRequest {
    private String question;
}
