package com.socialpulse.app.auth.service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.ErrorCode;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Your OTP Code for Social Pulse");

            String htmlContent = buildOtpHtml(otpCode);

            // true = HTML
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new AppException(ErrorCode.EMAIL_SENDS_FAILED);
        }
    }

    private String buildOtpHtml(String otpCode) {
        return """
      <table
        width="100%%"
        cellpadding="0"
        cellspacing="0"
        style="background: #f7f9fb; padding: 30px 0"
      >
        <tr>
          <td align="center">
            <table
              width="600"
              cellpadding="0"
              cellspacing="0"
              style="
                background: #ffffff;
                border-radius: 10px;
                padding: 30px;
                font-family: Arial, sans-serif;
              "
            >
              <tr>
                <td align="center">
                  <h2 style="color: #515f74; margin-bottom: 10px">
                    Verify Your Account
                  </h2>
                  <p style="color: #506076; margin: 0">
                    Use the OTP below to continue
                  </p>
                </td>
              </tr>

              <tr>
                <td style="padding: 20px 0">
                  <hr style="border: none; border-top: 1px solid #eee" />
                </td>
              </tr>

              <tr>
                <td align="center">
                  <div
                    style="
                      font-size: 36px;
                      font-weight: bold;
                      letter-spacing: 6px;
                      color: #515f74;
                    "
                  >
                    %s
                  </div>
                </td>
              </tr>

              <tr>
                <td style="padding-top: 20px; color: #333">
                  <p>This code will expire in <b>5 minutes</b>.</p>
                  <p>If you didn’t request this, you can safely ignore this email.</p>
                </td>
              </tr>

              <tr>
                <td
                  style="
                    padding-top: 30px;
                    font-size: 12px;
                    color: #999;
                    text-align: center;
                  "
                >
                  © 2026 Social Pulse. All rights reserved.
                </td>
              </tr>
            </table>
          </td>
        </tr>
      </table>
    """.formatted(otpCode);
    }
}
