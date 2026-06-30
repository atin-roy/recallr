package com.atinroy.recallr.domain.note;

import com.atinroy.recallr.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "note_links",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"source_note_id", "target_note_id"}
        )
)
public class NoteLink extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_note_id", nullable = false)
    private Note source;

    @ManyToOne(optional = false)
    @JoinColumn(name = "target_note_id", nullable = false)
    private Note target;

    private String label;
}
