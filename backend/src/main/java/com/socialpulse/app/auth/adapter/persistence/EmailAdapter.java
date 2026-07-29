package com.socialpulse.app.auth.adapter.persistence;
import org.springframework.stereotype.Component;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;

import com.socialpulse.app.auth.application.port.EmailPort;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.SystemCode;

import jakarta.mail.internet.MimeMessage;

@Component
public class EmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;

    public EmailAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void sendHtmlEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new AppException(SystemCode.EMAIL_SENDS_FAILED);
        }
    }
}


