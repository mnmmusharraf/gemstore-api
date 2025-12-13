package com.gemstore.backend.entities.lookup;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clarity_grades")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClarityGrade {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(nullable = false)
    private Integer rank;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
