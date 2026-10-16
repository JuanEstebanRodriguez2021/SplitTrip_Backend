package com.splittrip.service;

import com.splittrip.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends account emails. If spring.mail.* is not configured there is no JavaMailSender,
 * and the link is printed in the console instead (handy for local development).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String frontendUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${app.mail.from}") String from,
                        @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    public void sendVerificationEmail(User user, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        send(user, "Confirma tu cuenta de SplitTrip",
                "Hola " + user.getName() + ",\n\n"
                        + "Confirma tu correo para empezar a usar SplitTrip:\n"
                        + link + "\n\n"
                        + "El enlace vence en 24 horas. Si no creaste esta cuenta, ignora este mensaje.",
                "Verification", link);
    }

    public void sendPasswordResetEmail(User user, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        send(user, "Restablece tu clave de SplitTrip",
                "Hola " + user.getName() + ",\n\n"
                        + "Recibimos una solicitud para cambiar tu clave. Hazlo aquí:\n"
                        + link + "\n\n"
                        + "El enlace vence en 1 hora. Si no fuiste tú, ignora este mensaje: tu clave no cambiará.",
                "Password reset", link);
    }

    private void send(User user, String subject, String body, String kind, String link) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("Email sending is not configured. {} link for {}: {}", kind, user.getEmail(), link);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }
}