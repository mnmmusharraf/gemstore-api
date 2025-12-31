package com.gemstore.backend.mappers.user;


import com.gemstore.backend.dtos.auth.RegisterUserRequest;
import com.gemstore.backend.dtos.user.PublicUserDTO;
import com.gemstore.backend.dtos.user.UpdateProfileRequest;
import com.gemstore.backend.dtos.user.UserResponse;
import com.gemstore.backend.entities.user.User;
import org.mapstruct.*;

/**
 * UserMapper converts between User entity and various DTO layers.
 *
 * Design notes:
 * - We do NOT expose passwordHash in any outward DTO.
 * - Registration: incoming plain password should be encoded in service OR via @Context helper.
 * - Partial update for profile uses IGNORE null strategy so only provided fields update.
 * - PublicUserDTO intentionally hides email/provider/security fields.
 * - UserResponse is the "owner/self" or internal API view (still excludes passwordHash).
 *
 * If you use Spring Data auditing (CreatedDate/LastModifiedDate), do not map over those except from entity -> DTO.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    /* ===================== Entity -> DTO ===================== */

    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "provider", source = "provider")
    @Mapping(target = "emailVerified", source = "emailVerified")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "timezone", source = "timezone")
    @Mapping(target = "locale", source = "locale")

    // NEW
    @Mapping(target = "website", source = "website")
    @Mapping(target = "bio", source = "bio")
    @Mapping(target = "privateProfile", source = "privateProfile")
    @Mapping(target = "postsCount", source = "postsCount")
    @Mapping(target = "followersCount", source = "followersCount")
    @Mapping(target = "followingCount", source = "followingCount")
    UserResponse toUserResponse(User user);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    PublicUserDTO toPublicUserDTO(User user);

    /* ===================== DTO -> Entity (Creation) ===================== */

    /**
     * Maps registration request to a new User entity (WITHOUT encoding password).
     * Service layer should:
     *   1) Call mapper
     *   2) Encode raw password -> setPasswordHash()
     *   3) Set provider="LOCAL", role="USER", etc.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "passwordHash", ignore = true) // set later in service
    @Mapping(target = "provider", constant = "LOCAL")
    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "role", constant = "USER")
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "mfaEnabled", constant = "false")
    @Mapping(target = "timezone", ignore = true)
    @Mapping(target = "locale", ignore = true)
    User toEntity(RegisterUserRequest request);

    /* ===================== Partial Profile Update ===================== */

    /**
     * Applies non-null fields from UpdateProfileRequest onto an existing User entity.
     * Null fields are ignored (due to IGNORE strategy).
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "timezone", source = "timezone")
    @Mapping(target = "locale", source = "locale")

    @Mapping(target = "website", source = "website")
    @Mapping(target = "bio", source = "bio")
    @Mapping(target = "privateProfile", source = "privateProfile")

    void updateUserFromProfile(UpdateProfileRequest request, @MappingTarget User user);

    /* ===================== Password Change (Handled in Service) ===================== */

    // No direct mapping method: ChangePasswordRequest only carries current & new password.
    // Service should:
    //  1) Validate current password
    //  2) Encode new password
    //  3) user.setPasswordHash(encoded)
    //  4) user.markPasswordChanged()

    /* ===================== OPTIONAL: Mapper-driven password encoding ===================== */
    /**
     * If you prefer password encoding inside the mapper, you can supply a @Context PasswordEncoder wrapper.
     * Example usage:
     *   User user = userMapper.toEntity(request, encoderContext);
     * Where encoderContext is a bean exposing encode(raw).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "passwordHash", expression = "java( passwordEncoderContext.encode(request.getPassword()) )")
    @Mapping(target = "provider", constant = "LOCAL")
    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "role", constant = "USER")
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "mfaEnabled", constant = "false")
    @Mapping(target = "timezone", ignore = true)
    @Mapping(target = "locale", ignore = true)
    User toEntityWithEncoding(RegisterUserRequest request,
                              @Context PasswordEncoderContext passwordEncoderContext);

    /* ===================== After-Mapping Hook (Optional Fallback) ===================== */

    /**
     * Optional fallback: if displayName was null in registration but you want automatic fallback here
     * (instead of relying only on @PrePersist in the entity).
     */
    @AfterMapping
    default void fillDisplayName(@MappingTarget User user) {
        if (user.getDisplayName() == null) {
            if (user.getFirstName() != null || user.getLastName() != null) {
                String dn = (user.getFirstName() != null ? user.getFirstName() : "") +
                        (user.getLastName() != null ? " " + user.getLastName() : "");
                dn = dn.trim();
                if (!dn.isEmpty()) {
                    user.setDisplayName(dn);
                    return;
                }
            }
            if (user.getUsername() != null) {
                user.setDisplayName(user.getUsername());
            } else if (user.getEmail() != null) {
                user.setDisplayName(user.getEmail().split("@")[0]);
            }
        }
    }

    /* ===================== Context Interface for Password Encoding ===================== */

    /**
     * Context adapter so MapStruct can call a password encoder without tying directly
     * to Spring Security's PasswordEncoder (keeps mapper testable).
     */
    interface PasswordEncoderContext {
        String encode(String raw);
    }
}