package com.atinroy.recallr.domain.flashcard;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.domain.subject.Subject;
import com.atinroy.recallr.domain.topic.Topic;
import com.atinroy.recallr.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "basic_flashcards")
public class BasicFlashcard extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(nullable = false)
    private boolean reverse;
}
