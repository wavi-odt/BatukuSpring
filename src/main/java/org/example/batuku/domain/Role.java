package org.example.batuku.domain;

import jakarta.persistence.*;

/**
 * Representa um papel (role) no sistema.
 *
 * Valores esperados: ROLE_FAN, ROLE_ARTIST, ROLE_ADMIN
 *
 * Usamos uma tabela separada (igual ao projeto do professor) para que
 * o Spring Security consiga ler as autoridades de forma padronizada.
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Único e obrigatório — ex: "ROLE_FAN"
    @Column(nullable = false, unique = true)
    private String name;

    // ── Getters / Setters ───────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
