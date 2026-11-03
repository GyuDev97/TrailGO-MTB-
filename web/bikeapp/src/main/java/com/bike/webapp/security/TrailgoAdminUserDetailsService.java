package com.bike.webapp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class TrailgoAdminUserDetailsService implements UserDetailsService {

    @Value("${trailgo.admin.username:}")
    private String adminUsername;

    @Value("${trailgo.admin.password-hash:}")
    private String adminPasswordHash;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (adminUsername == null || adminUsername.isEmpty()
                || adminPasswordHash == null || !adminPasswordHash.startsWith("$2")
                || !adminUsername.equals(username)) {
            throw new UsernameNotFoundException("TrailGO admin account is not configured");
        }

        return User.withUsername(adminUsername)
                .password(adminPasswordHash)
                .roles("ADMIN")
                .build();
    }
}
