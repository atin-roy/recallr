package com.atinroy.recallr.domain.mcq;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.domain.subject.Subject;
import com.atinroy.recallr.domain.topic.Topic;
import com.atinroy.recallr.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mcqs")
@Getter
@Setter
public class MCQ extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column
    private String question;

    @ElementCollection
    private List<String> options = new ArrayList<>();

    @Column
    private int correctOptionIndex;

    @Column(length = 1000)
    private String explanation;
}
