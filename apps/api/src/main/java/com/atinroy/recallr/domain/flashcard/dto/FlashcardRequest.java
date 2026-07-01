package com.atinroy.recallr.domain.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BasicFlashcardRequest.class, name = "BASIC"),
    @JsonSubTypes.Type(value = MCQFlashcardRequest.class, name = "MCQ"),
    @JsonSubTypes.Type(value = ClozeFlashcardRequest.class, name = "CLOZE"),
    @JsonSubTypes.Type(value = TrueFalseFlashcardRequest.class, name = "TRUE_FALSE"),
    @JsonSubTypes.Type(value = MatchingFlashcardRequest.class, name = "MATCHING"),
    @JsonSubTypes.Type(value = OrderingFlashcardRequest.class, name = "ORDERING"),
    @JsonSubTypes.Type(value = ShortAnswerFlashcardRequest.class, name = "SHORT_ANSWER"),
})
@Getter
@Setter
public abstract class FlashcardRequest {
    private String question;
}
