package org.example.batuku.utils;

import org.example.batuku.exception.SpotifyApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Traducao de excecoes em respostas HTTP: codigo de estado e corpo
 * {"error": "..."} que o frontend mostra ao utilizador.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static HttpStatus estadoDe(String metodo, Class<?> parametro) throws NoSuchMethodException {
        var m = parametro == null
                ? GlobalExceptionHandler.class.getMethod(metodo)
                : GlobalExceptionHandler.class.getMethod(metodo, parametro);
        return m.getAnnotation(ResponseStatus.class).value();
    }

    @Test
    @DisplayName("Elemento nao encontrado -> 404 com mensagem generica")
    void naoEncontrado() throws Exception {
        Map<String, String> corpo = handler.handleNotFound();

        assertEquals("Not found", corpo.get("error"));
        assertEquals(HttpStatus.NOT_FOUND, estadoDe("handleNotFound", null));
    }

    @Test
    @DisplayName("Argumento invalido -> 400 com a mensagem da excecao")
    void argumentoInvalido() throws Exception {
        Map<String, String> corpo = handler.handleIllegalArgument(
                new IllegalArgumentException("O ficheiro de audio e obrigatorio."));

        assertEquals("O ficheiro de audio e obrigatorio.", corpo.get("error"));
        assertEquals(HttpStatus.BAD_REQUEST, estadoDe("handleIllegalArgument", IllegalArgumentException.class));
    }

    @Test
    @DisplayName("Estado invalido -> 409 com a mensagem da excecao")
    void estadoInvalido() throws Exception {
        Map<String, String> corpo = handler.handleIllegalState(
                new IllegalStateException("Ja existe um pedido pendente para este email."));

        assertEquals("Ja existe um pedido pendente para este email.", corpo.get("error"));
        assertEquals(HttpStatus.CONFLICT, estadoDe("handleIllegalState", IllegalStateException.class));
    }

    @Test
    @DisplayName("Erro da API do Spotify -> reencaminha o codigo HTTP recebido")
    void erroDoSpotify() {
        ResponseEntity<Map<String, String>> r = handler.handleSpotifyException(
                new SpotifyApiException("Limite de pedidos excedido.", 429));

        assertEquals(429, r.getStatusCode().value());
        assertEquals("Limite de pedidos excedido.", r.getBody().get("error"));
    }

    @Test
    @DisplayName("Spotify indisponivel (502) mantem o codigo 502")
    void spotifyIndisponivel() {
        ResponseEntity<Map<String, String>> r = handler.handleSpotifyException(
                new SpotifyApiException("Spotify indisponivel.", 502));

        assertEquals(502, r.getStatusCode().value());
    }

    @Test
    @DisplayName("Validacao falhada -> 400 com um erro por campo, separados por ponto e virgula")
    void validacao() throws Exception {
        BindingResult resultado = new BeanPropertyBindingResult(new Object(), "pedido");
        resultado.addError(new FieldError("pedido", "email", "tem de ser um email valido"));
        resultado.addError(new FieldError("pedido", "username", "nao pode estar vazio"));
        MethodArgumentNotValidException excecao = mock(MethodArgumentNotValidException.class);
        when(excecao.getBindingResult()).thenReturn(resultado);

        Map<String, String> corpo = handler.handleValidation(excecao);

        assertEquals("email: tem de ser um email valido; username: nao pode estar vazio", corpo.get("error"));
        assertEquals(HttpStatus.BAD_REQUEST, estadoDe("handleValidation", MethodArgumentNotValidException.class));
    }

    @Test
    @DisplayName("Validacao com um unico erro nao tem separador")
    void validacaoUmErro() {
        BindingResult resultado = new BeanPropertyBindingResult(new Object(), "pedido");
        resultado.addError(new FieldError("pedido", "password", "minimo de 8 caracteres"));
        MethodArgumentNotValidException excecao = mock(MethodArgumentNotValidException.class);
        when(excecao.getBindingResult()).thenReturn(resultado);

        assertEquals("password: minimo de 8 caracteres", handler.handleValidation(excecao).get("error"));
    }
}
