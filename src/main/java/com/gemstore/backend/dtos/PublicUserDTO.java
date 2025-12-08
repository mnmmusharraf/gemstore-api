package com.gemstore.backend.dtos;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicUserDTO {
    private Long id;
    private String displayName;
    private String username;
    private String avatarUrl;
}
