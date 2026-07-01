package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("MATCHING")
@Getter
@Setter
public class MatchingFlashcard extends Flashcard {

    @ElementCollection
    @CollectionTable(name = "flashcard_matching_pairs", joinColumns = @JoinColumn(name = "flashcard_id"))
    @OrderColumn(name = "pair_order")
    private List<MatchingPair> pairs = new ArrayList<>();
}
