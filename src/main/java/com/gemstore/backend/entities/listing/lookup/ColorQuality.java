package com.gemstore.backend.entities.listing.lookup;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "color_qualities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColorQuality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(nullable = false)
    private Integer rank;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
