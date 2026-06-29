package com.atinroy.recallr.topic;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.subject.Subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "topics")
@Getter
@Setter
public class Topic extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(length = 200, nullable = false)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;
}
