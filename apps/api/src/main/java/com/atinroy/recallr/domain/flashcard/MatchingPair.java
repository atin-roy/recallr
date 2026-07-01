package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class MatchingPair {

    @Column(name = "prompt", length = 500)
    private String prompt;

    @Column(name = "match_value", length = 500)
    private String matchValue;
}
