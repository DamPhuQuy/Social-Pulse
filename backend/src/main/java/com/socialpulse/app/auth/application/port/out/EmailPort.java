package com.socialpulse.app.auth.application.port.out;

public interface EmailPort {
    void sendHtmlEmail(String to, String subject, String body);
}
