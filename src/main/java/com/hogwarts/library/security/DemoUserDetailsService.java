package com.hogwarts.library.security;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Profile("demo")
public class DemoUserDetailsService implements UserDetailsService {

    public static final String USERNAME = "visitante-demo";
    public static final String ROLE = "DEMO";

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (!USERNAME.equals(username)) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }
        return User.withUsername(USERNAME).password("").roles(ROLE).build();
    }
}
