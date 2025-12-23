package com.gemstore.backend.services.auth;


import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(identifier)
                .or(() -> userRepository.findByUsernameIgnoreCase(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.isSoftDeleted()) throw new UsernameNotFoundException("Account deleted");
        if (user.isLocked()) throw new LockedException("Account locked until " + user.getLockedUntil());
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) throw new DisabledException("Account not active");

        return CustomUserDetails.from(user);
    }
}