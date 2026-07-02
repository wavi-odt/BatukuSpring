package org.example.batuku.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO recebido no endpoint POST /api/auth/register
 *
 * Usamos anotações de validação para garantir dados corretos
 * antes de chegar ao serviço. Se alguma falhar, o Spring devolve
 * automaticamente um 400 Bad Request com a mensagem de erro.
 */
public class RegisterRequest {

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "O username é obrigatório")
    @Size(min = 3, max = 50, message = "O username deve ter entre 3 e 50 caracteres")
    private String username;

    @NotBlank(message = "A password é obrigatória")
    @Size(min = 8, message = "A password deve ter pelo menos 8 caracteres")
    private String password;

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
    private String name;

    // País é opcional — pode não ser enviado
    private String country;

    // Papel pretendido: "FAN" ou "ARTIST" — se omitido, fica FAN por defeito
    private String userRole;

    // ── Getters / Setters ───────────────────────────────────────────

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
}
