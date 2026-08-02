package org.example.batuku.services;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.User;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final String FROM = "batuku.suporte@gmail.com";

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendClaimDoubtfulEmail(User user, ArtistProfile profile) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM);
        message.setTo(user.getEmail());
        message.setSubject("O teu pedido de reclamação de perfil precisa de mais informação");
        message.setText(
            "Olá " + user.getName() + ",\n\n" +
            "Recebemos o teu pedido de reclamação do perfil \"" + profile.getName() +
            "\" na plataforma Batuku.\n\n" +
            "Após analisar os documentos submetidos, precisamos de mais informação ou " +
            "esclarecimentos para prosseguir com a verificação.\n\n" +
            "Por favor responde a este email com os esclarecimentos necessários, " +
            "indicando o nome do perfil em questão.\n\n" +
            "Equipa Batuku\n" +
            FROM
        );
        mailSender.send(message);
    }
}
