package com.atinroy.recallr.domain.deck;

import com.atinroy.recallr.domain.deck.dto.DeckRequest;
import com.atinroy.recallr.domain.deck.dto.DeckResponse;
import com.atinroy.recallr.domain.notebook.Notebook;
import com.atinroy.recallr.domain.user.User;
import org.springframework.stereotype.Component;

@Component
public class DeckMapper {

    public Deck toEntity(DeckRequest request, User user, Notebook notebook) {
        Deck deck = new Deck();
        deck.setUser(user);
        deck.setNotebook(notebook);
        deck.setName(request.name());
        deck.setDescription(request.description());
        return deck;
    }

    public DeckResponse toResponse(Deck deck) {
        return new DeckResponse(
                deck.getId(),
                deck.getNotebook() != null ? deck.getNotebook().getId() : null,
                deck.getName(),
                deck.getDescription(),
                deck.getCreatedAt(),
                deck.getUpdatedAt()
        );
    }
}
