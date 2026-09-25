package org.example.batuku.services;

import jakarta.mail.internet.MimeMessage;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.User;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private static final String FROM      = "batuku.suporte@gmail.com";
    private static final String FROM_NAME = "Batuku";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender     = mailSender;
        this.templateEngine = templateEngine;
    }

    // ── Envio genérico ───────────────────────────────────────────────────

    private void send(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process("emails/" + template, ctx);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(FROM, FROM_NAME);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar email '" + template + "': " + e.getMessage(), e);
        }
    }

    // ── Emails públicos ──────────────────────────────────────────────────

    public void sendWelcomeEmail(User user) {
        Context ctx = new Context();
        ctx.setVariable("name", user.getName());
        send(user.getEmail(), "Bem-vindo ao Batuku!", "welcome", ctx);
    }

    public void sendArtistPendingEmail(User user) {
        Context ctx = new Context();
        ctx.setVariable("name", user.getName());
        send(user.getEmail(), "Registo recebido, aguarda validação", "artist-pending", ctx);
    }

    public void sendClaimVerifiedEmail(User user, ArtistProfile profile) {
        Context ctx = new Context();
        ctx.setVariable("name", user.getName());
        ctx.setVariable("artistName", profile.getName());
        send(user.getEmail(), "Perfil verificado, já podes publicar música!", "claim-verified", ctx);
    }

    public void sendClaimDoubtfulEmail(User user, ArtistProfile profile) {
        Context ctx = new Context();
        ctx.setVariable("name", user.getName());
        ctx.setVariable("artistName", profile.getName());
        send(user.getEmail(), "O teu pedido precisa de mais informação", "claim-doubtful", ctx);
    }
}
