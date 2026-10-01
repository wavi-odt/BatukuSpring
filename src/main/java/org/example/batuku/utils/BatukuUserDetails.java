package org.example.batuku.utils;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class BatukuUserDetails extends User {

    private final String name;

    public BatukuUserDetails(String email, String password, boolean enabled,
                             Collection<? extends GrantedAuthority> authorities, String name) {
        super(email, password, enabled, true, true, true, authorities);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
