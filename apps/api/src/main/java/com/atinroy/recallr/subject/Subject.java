package com.atinroy.recallr.subject;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subjects")
@Getter
@Setter
public class Subject extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 200, nullable = false)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;
}
