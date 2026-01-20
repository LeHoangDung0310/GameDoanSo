package com.gamedoanso.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer score = 0;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer turns = 0;
}
