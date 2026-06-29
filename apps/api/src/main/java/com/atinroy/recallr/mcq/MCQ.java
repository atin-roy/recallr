package com.atinroy.recallr.mcq;

import com.atinroy.recallr.common.BaseEntity;
import com.atinroy.recallr.user.User;
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

    @Column
    private String question;

    @ElementCollection
    private List<String> options = new ArrayList<>();

    @Column
    private int correctOptionIndex;

    @Column(length = 1000)
    private String explanation;
}
