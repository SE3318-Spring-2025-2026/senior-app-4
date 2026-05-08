package com.spms.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import com.spms.backend.exception.EmailException;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String fullName, String resetToken) {
        String resetUrl = frontendUrl + "/auth/reset-password?token=" + resetToken;

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("SPMS — Password Reset Request");
            helper.setText(buildHtml(fullName, resetUrl), true);
            mailSender.send(mime);
        } catch (MessagingException e) {
            throw new EmailException("Failed to send password reset email.", e);
        }
    }

    private String buildHtml(String fullName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Password Reset</title>
            </head>
            <body style="margin:0;padding:0;background-color:#030712;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#030712;padding:48px 16px;">
                <tr>
                  <td align="center">

                    <!-- Header -->
                    <table width="480" cellpadding="0" cellspacing="0">
                      <tr>
                        <td align="center" style="padding-bottom:32px;">
                          <p style="margin:0;font-size:13px;font-weight:600;letter-spacing:0.15em;color:#6b7280;text-transform:uppercase;">
                            Senior Project Management System
                          </p>
                          <div style="margin-top:8px;width:40px;height:2px;background-color:#2563eb;border-radius:2px;"></div>
                        </td>
                      </tr>
                    </table>

                    <!-- Card -->
                    <table width="480" cellpadding="0" cellspacing="0"
                           style="background-color:#111827;border:1px solid rgba(255,255,255,0.08);border-radius:16px;overflow:hidden;">
                      <tr>
                        <td style="padding:40px 40px 32px;">

                          <p style="margin:0 0 8px;font-size:20px;font-weight:600;color:#f9fafb;text-align:center;">
                            Password Reset Request
                          </p>
                          <p style="margin:0 0 28px;font-size:14px;color:#9ca3af;text-align:center;line-height:1.6;">
                            Hello %s, we received a request to reset your password.<br/>
                            Click the button below to set a new password for your account.
                          </p>

                          <!-- Button -->
                          <table width="100%%" cellpadding="0" cellspacing="0">
                            <tr>
                              <td align="center" style="padding-bottom:28px;">
                                <a href="%s"
                                   style="display:inline-block;padding:14px 36px;background-color:#2563eb;color:#ffffff;
                                          font-size:14px;font-weight:600;text-decoration:none;border-radius:10px;
                                          letter-spacing:0.03em;">
                                  SET NEW PASSWORD
                                </a>
                              </td>
                            </tr>
                          </table>

                          <!-- Divider -->
                          <div style="border-top:1px solid rgba(255,255,255,0.06);margin-bottom:24px;"></div>

                          <p style="margin:0;font-size:12px;color:#6b7280;text-align:center;line-height:1.6;">
                            This link can only be used once.<br/>
                            If you did not request a password reset, you can safely ignore this email.
                          </p>

                        </td>
                      </tr>
                    </table>

                    <!-- Footer -->
                    <table width="480" cellpadding="0" cellspacing="0">
                      <tr>
                        <td align="center" style="padding-top:24px;">
                          <p style="margin:0;font-size:11px;color:#374151;">
                            Yaşar University &middot; Senior Project Management System
                          </p>
                        </td>
                      </tr>
                    </table>

                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(fullName, resetUrl);
    }
}
