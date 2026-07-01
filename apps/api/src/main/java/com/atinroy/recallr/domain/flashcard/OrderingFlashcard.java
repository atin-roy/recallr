package com.atinroy.recallr.domain.flashcard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("ORDERING")
@Getter
@Setter
public class OrderingFlashcard extends Flashcard {

    @ElementCollection
    @CollectionTable(name = "flashcard_ordering_items", joinColumns = @JoinColumn(name = "flashcard_id"))
    @Column(name = "item")
    @OrderColumn(name = "item_order")
    private List<String> items = new ArrayList<>();
}
