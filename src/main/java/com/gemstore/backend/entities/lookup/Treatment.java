package com.gemstore.backend.entities.lookup;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "treatments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Treatment {

    @Id
    @GeneratedValue(strategy = GenerationType. IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
