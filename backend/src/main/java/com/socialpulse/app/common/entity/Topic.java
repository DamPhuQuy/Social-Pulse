package com.socialpulse.app.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "topics")
@Getter
@Builder
public class Topic {
    @Id
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;
}
