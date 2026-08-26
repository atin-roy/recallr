package com.atinroy.recallr.domain.flashcard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchingPairDto {
    private String prompt;
    private String matchValue;
}
