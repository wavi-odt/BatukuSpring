package org.example.batuku.utils;

import java.io.Serializable;

// ─── FICHEIRO DO PROFESSOR — NÃO ALTERAR ───────────────────────────
public class JwtResponse implements Serializable {

    private static final long serialVersionUID = -8091879091924046844L;
    private final String jwttoken;

    public JwtResponse(String jwttoken) {
        this.jwttoken = jwttoken;
    }

    public String getToken() {
        return this.jwttoken;
    }
}
