package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("MCQ")
@Getter
@Setter
public class MCQFlashcard extends Flashcard {

    @ElementCollection
    @CollectionTable(name = "flashcard_options", joinColumns = @JoinColumn(name = "flashcard_id"))
    @Column(name = "options")
    private List<String> options = new ArrayList<>();

    @Column
    private int correctOptionIndex;

    @Column(length = 1000)
    private String explanation;
}
