package org.example.batuku.services;

import org.example.batuku.domain.Role;
import org.example.batuku.domain.User;
import org.example.batuku.dto.RegisterRequest;
import org.example.batuku.repository.RoleRepository;
import org.example.batuku.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Serviço responsável pelo registo de novos utilizadores.
 *
 * A lógica de LOGIN não está aqui — é tratada diretamente pelo
 * JwtAuthenticationController (igual ao projeto do professor),
 * que usa o CustomAuthenticationManager + JwtTokenUtil.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Regista um novo utilizador como FAN ou ARTIST conforme o campo userRole do pedido.
     * Se userRole for omitido ou inválido, fica FAN por defeito.
     */
    @Transactional
    public User register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Este email já está em uso.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Este username já está em uso.");
        }

        // 2) Determinar o papel pedido — só FAN ou ARTIST são permitidos no registo
        boolean wantsArtist = "ARTIST".equalsIgnoreCase(request.getUserRole());
        User.UserRole userRole = wantsArtist ? User.UserRole.ARTIST : User.UserRole.FAN;
        String roleName = wantsArtist ? "ROLE_ARTIST" : "ROLE_FAN";

        Role springRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException(roleName + " não encontrada. Verifica o SeedRoles."));

        // 3) Construir o utilizador
        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setUsername(request.getUsername().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName().trim());
        user.setCountry(request.getCountry());
        user.setUserRole(userRole);          // papel de negócio
        user.setRoles(Set.of(springRole));   // role do Spring Security
        user.setEnabled(true);

        return userRepository.save(user);
    }
}
