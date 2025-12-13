package com.gemstore.backend.entities.listing.lookup;

import jakarta.persistence.*;
import lombok.*;



@Entity
@Table(name = "gemstone_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GemstoneType {

    @Id
    @GeneratedValue(strategy = GenerationType. IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 20)
    private String category;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
