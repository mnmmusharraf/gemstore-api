//package com.gemstore.backend.security;
//
//import com.gemstore.backend.entities.user.User;
//import lombok. Getter;
//import org.springframework. security.core.GrantedAuthority;
//import org.springframework. security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.util.Collection;
//import java.util.List;
//
//@Getter
//public class CustomUserDetails implements UserDetails {
//
//    private final Long id;
//    private final String email;
//    private final String username;
//    private final String password;
//    private final Collection<?  extends GrantedAuthority> authorities;
//    private final boolean enabled;
//
//    public CustomUserDetails(User user) {
//        this.id = user.getId();
//        this.email = user.getEmail();
//        this.username = user. getUsername() != null ? user.getUsername() : user.getEmail();
//        this.password = user.getPasswordHash();
//        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
//        this.enabled = "ACTIVE".equals(user.getStatus());
//    }
//
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return authorities;
//    }
//
//    @Override
//    public String getPassword() {
//        return password;
//    }
//
//    @Override
//    public String getUsername() {
//        return username;
//    }
//
//    @Override
//    public boolean isAccountNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isAccountNonLocked() {
//        return enabled;
//    }
//
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isEnabled() {
//        return enabled;
//    }
//}

package com.gemstore.backend.security;

import com.gemstore.backend.entities.user.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    /**
     * Constructor for database-loaded users (existing - keep this).
     */
    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername() != null ? user.getUsername() : user.getEmail();
        this.password = user.getPasswordHash();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        this.enabled = "ACTIVE".equals(user.getStatus());
    }

    /**
     * ✅ NEW: Constructor for JWT-based authentication.
     * Used by JwtFilter to create user details from token claims.
     * This is CRITICAL for fixing the "user is NULL" issue!
     */
    public CustomUserDetails(Long id, String username, String role) {
        this.id = id;
        this.username = username;
        this.email = username; // Use username as email fallback for JWT auth
        this.password = ""; // No password needed for JWT auth
        // Handle role with or without ROLE_ prefix
        String authorityRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        this.authorities = List.of(new SimpleGrantedAuthority(authorityRole));
        this.enabled = true;
    }

    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(user);
    }

    /**
     * ✅ NEW: Constructor with full details (optional, for flexibility).
     */
    public CustomUserDetails(Long id, String username, String email, String password,
                             String role, boolean enabled) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        String authorityRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        this.authorities = List.of(new SimpleGrantedAuthority(authorityRole));
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get user ID - CRITICAL for controller authentication.
     */
    public Long getId() {
        return id;
    }

    /**
     * Get email address.
     */
    public String getEmail() {
        return email;
    }
}