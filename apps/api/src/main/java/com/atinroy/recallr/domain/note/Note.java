package com.atinroy.recallr.domain.note;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.domain.notebook.Notebook;
import com.atinroy.recallr.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "notes")
@Getter
@Setter
public class Note extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    @Column(length = 100)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<NoteLink> outgoingLinks = new HashSet<>();

    @OneToMany(mappedBy = "target")
    private Set<NoteLink> incomingLinks = new HashSet<>();
}
