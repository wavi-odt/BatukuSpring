package org.example.batuku.utils;

import java.io.Serializable;

// ─── FICHEIRO DO PROFESSOR, NAO ALTERAR ────────────────────────────
public class JwtResponse implements Serializable {

    private static final long serialVersionUID = -8091879091924046844L;
    private final String jwttoken;
    private final String refreshToken;

    public JwtResponse(String jwttoken) {
        this.jwttoken     = jwttoken;
        this.refreshToken = null;
    }

    public JwtResponse(String jwttoken, String refreshToken) {
        this.jwttoken     = jwttoken;
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return this.jwttoken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }
}
