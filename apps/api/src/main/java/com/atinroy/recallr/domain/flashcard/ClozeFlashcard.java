package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("CLOZE")
@Getter
@Setter
public class ClozeFlashcard extends Flashcard {

    @ElementCollection
    @CollectionTable(name = "flashcard_cloze_answers", joinColumns = @JoinColumn(name = "flashcard_id"))
    @Column(name = "answer")
    @OrderColumn(name = "answer_order")
    private List<String> answers = new ArrayList<>();
}
