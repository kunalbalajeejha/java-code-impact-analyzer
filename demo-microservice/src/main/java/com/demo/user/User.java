package com.demo.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String tier;  // GOLD | SILVER | BRONZE

    public User(String name, String email, String tier) {
        this.name  = name;
        this.email = email;
        this.tier  = tier;
    }
}
