package org.example.batuku.utils;

import org.example.batuku.domain.Role;
import org.example.batuku.domain.User;
import org.example.batuku.repository.UserRepository;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// ─── FICHEIRO DO PROFESSOR — NÃO ALTERAR ───────────────────────────
@Component
public class CustomAuthenticationManager implements AuthenticationManager {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthenticationManager(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        User user = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Wrong Email"));

        if (!passwordEncoder.matches(authentication.getCredentials().toString(), user.getPassword())) {
            throw new BadCredentialsException("Wrong Password");
        }

        List<GrantedAuthority> grantedAuthorityList = new ArrayList<>();
        for (Role role : user.getRoles()) {
            grantedAuthorityList.add(new SimpleGrantedAuthority(role.getName()));
        }

        return new UsernamePasswordAuthenticationToken(
                authentication.getPrincipal(),
                authentication.getCredentials(),
                grantedAuthorityList
        );
    }
}
