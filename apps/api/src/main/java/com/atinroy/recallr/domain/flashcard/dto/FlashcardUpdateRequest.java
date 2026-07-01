package com.atinroy.recallr.domain.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BasicFlashcardUpdateRequest.class, name = "BASIC"),
    @JsonSubTypes.Type(value = MCQFlashcardUpdateRequest.class, name = "MCQ"),
    @JsonSubTypes.Type(value = ClozeFlashcardUpdateRequest.class, name = "CLOZE"),
    @JsonSubTypes.Type(value = TrueFalseFlashcardUpdateRequest.class, name = "TRUE_FALSE"),
    @JsonSubTypes.Type(value = MatchingFlashcardUpdateRequest.class, name = "MATCHING"),
    @JsonSubTypes.Type(value = OrderingFlashcardUpdateRequest.class, name = "ORDERING"),
})
@Getter
@Setter
public abstract class FlashcardUpdateRequest {
    private String question;
}
