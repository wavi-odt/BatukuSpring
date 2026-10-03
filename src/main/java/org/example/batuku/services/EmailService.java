package org.example.batuku.services;

import jakarta.mail.internet.MimeMessage;
import org.example.batuku.config.MailProperties;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProps;
    private final LoginTokenService loginTokenService;
    private final RestClient restClient;

    @Value("${batuku.app.base-url}")
    private String appBaseUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine,
                        MailProperties mailProps, LoginTokenService loginTokenService) {
        this.mailSender        = mailSender;
        this.templateEngine    = templateEngine;
        this.mailProps         = mailProps;
        this.loginTokenService = loginTokenService;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    // ── Envio genérico ───────────────────────────────────────────────────

    private void send(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process("emails/" + template, ctx);
            if ("brevo".equalsIgnoreCase(mailProps.getProvider())) {
                sendViaBrevo(to, subject, html);
            } else {
                sendViaSmtp(to, subject, html);
            }
        } catch (RestClientResponseException e) {
            log.error("Brevo HTTP {} ao enviar '{}' para {}: {}",
                    e.getStatusCode().value(), template, to, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Falha ao enviar email '{}' para {}: {}", template, to, e.getMessage());
        }
    }

    private void sendViaSmtp(String to, String subject, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailProps.getFromEmail(), mailProps.getFromName());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    private void sendViaBrevo(String to, String subject, String html) {
        Map<String, Object> payload = Map.of(
                "sender",      Map.of("name", mailProps.getFromName(), "email", mailProps.getFromEmail()),
                "to",          List.of(Map.of("email", to)),
                "subject",     subject,
                "htmlContent", html
        );
        restClient.post()
                .uri("https://api.brevo.com/v3/smtp/email")
                .header("api-key", mailProps.getBrevo().getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    // ── Emails públicos ──────────────────────────────────────────────────

    public void sendEmailVerificationEmail(String email, String name, String verificationUrl) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        ctx.setVariable("verificationUrl", verificationUrl);
        send(email, "Verifica o teu email - Batuku", "verify-email", ctx);
    }

    public void sendWelcomeEmail(User user) {
        boolean isOAuth   = user.getPassword() == null || user.getPassword().startsWith("{OAUTH}");
        String redirectTo = isOAuth ? "/settings" : "/home";
        String magicToken = loginTokenService.generate(user, redirectTo);
        Context ctx = new Context();
        ctx.setVariable("name",      user.getName());
        ctx.setVariable("magicLink", appBaseUrl + "/auto-login?token=" + magicToken);
        ctx.setVariable("isOAuth",   isOAuth);
        send(user.getEmail(), "Bem-vindo ao Batuku!", "welcome", ctx);
    }

    public void sendArtistPendingEmail(User user) {
        Context ctx = new Context();
        ctx.setVariable("name", user.getName());
        send(user.getEmail(), "Registo recebido, aguarda validação", "artist-pending", ctx);
    }

    public void sendArtistPendingEmailToAddress(String email, String name) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        send(email, "Registo recebido, aguarda validação", "artist-pending", ctx);
    }

    public void sendArtistClaimDoubtfulToAddress(String email, String name, String artistName) {
        Context ctx = new Context();
        ctx.setVariable("name", name);
        ctx.setVariable("artistName", artistName);
        send(email, "O teu pedido precisa de mais informação", "claim-doubtful", ctx);
    }

    public void sendClaimVerifiedEmail(User user, ArtistProfile profile) {
        String magicToken = loginTokenService.generate(user, "/dashboard");
        Context ctx = new Context();
        ctx.setVariable("name", user.getName());
        ctx.setVariable("artistName", profile.getName());
        ctx.setVariable("magicLink", appBaseUrl + "/auto-login?token=" + magicToken);
        send(user.getEmail(), "Perfil verificado, já podes publicar música!", "claim-verified", ctx);
    }

    public void sendClaimDoubtfulEmail(User user, String artistName) {
        Context ctx = new Context();
        ctx.setVariable("name", user.getName());
        ctx.setVariable("artistName", artistName);
        send(user.getEmail(), "O teu pedido precisa de mais informação", "claim-doubtful", ctx);
    }
}
