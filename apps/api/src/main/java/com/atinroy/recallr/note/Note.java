package com.atinroy.recallr.note;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.user.User;
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
