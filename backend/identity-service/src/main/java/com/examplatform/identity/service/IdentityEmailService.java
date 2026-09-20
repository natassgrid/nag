/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.identity.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:no-reply@exam-platform.gov.in}")
    private String mailFrom;

    @Value("${app.frontend.admin-url:http://localhost:4200}")
    private String adminPortalUrl;

    /**
     * Sends candidate email OTP verification email.
     */
    public boolean sendCandidateEmailOtp(String recipientEmail, String otpCode, String candidateName) {
        String subject = "National Assessment Grid - Verify Your Email Address";
        String displayName = (candidateName != null && !candidateName.isBlank()) ? candidateName : "Candidate";

        String htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Email Verification</title>
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; }
                .container { max-width: 540px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; }
                .header { background: #1e3a8a; color: #ffffff; padding: 24px; text-align: center; }
                .header h1 { margin: 0; font-size: 20px; font-weight: 700; letter-spacing: 0.5px; }
                .header p { margin: 6px 0 0 0; font-size: 13px; opacity: 0.85; }
                .content { padding: 32px 28px; color: #334155; }
                .otp-box { background: #f1f5f9; border: 2px dashed #94a3b8; border-radius: 8px; padding: 18px; text-align: center; margin: 24px 0; }
                .otp-code { font-family: monospace; font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #1e3a8a; margin: 0; }
                .warning { background: #fef2f2; border-left: 4px solid #ef4444; padding: 12px 16px; margin: 20px 0; border-radius: 0 6px 6px 0; font-size: 13px; color: #991b1b; }
                .footer { background: #f8fafc; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>National Assessment Grid (NAG)</h1>
                  <p>Digital Public Infrastructure for Standardized Examinations</p>
                </div>
                <div class="content">
                  <p>Dear <strong>%s</strong>,</p>
                  <p>Thank you for registering on the National Assessment Grid. Please use the following One-Time Password (OTP) to verify your email address:</p>
                  <div class="otp-box">
                    <div class="otp-code">%s</div>
                  </div>
                  <p>This verification code is valid for <strong>10 minutes</strong>.</p>
                  <div class="warning">
                    <strong>Security Notice:</strong> Never share this OTP with anyone. NAG administrators will never request your verification code or password.
                  </div>
                  <p>If you did not initiate this registration, please disregard this email.</p>
                </div>
                <div class="footer">
                  &copy; 2025 National Assessment Grid (NAG). All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """.formatted(escapeHtml(displayName), escapeHtml(otpCode));

        String textContent = "Dear " + displayName + ",\n\n"
                + "Your National Assessment Grid (NAG) email verification OTP is: " + otpCode + "\n\n"
                + "This OTP is valid for 10 minutes.\n"
                + "Security Notice: Never share this OTP with anyone.\n\n"
                + "If you did not request this, please disregard this email.\n";

        return sendMimeEmail(recipientEmail, subject, htmlContent, textContent);
    }

    /**
     * Sends invitation email to onboard a new Administrator or Staff member.
     */
    public boolean sendAdminInvitationEmail(String recipientEmail, String fullName, String roles, String invitationToken) {
        String subject = "Invitation to Join National Assessment Grid (NAG) Admin Portal";
        String inviteLink = adminPortalUrl + "/auth/accept-invite?token=" + invitationToken;

        String htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>NAG Portal Invitation</title>
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; }
                .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; }
                .header { background: #0f172a; color: #ffffff; padding: 24px; text-align: center; }
                .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
                .content { padding: 32px 28px; color: #334155; line-height: 1.6; }
                .btn-container { text-align: center; margin: 30px 0; }
                .btn { display: inline-block; background: #2563eb; color: #ffffff !important; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: 600; font-size: 15px; }
                .roles-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px 16px; margin: 18px 0; font-size: 14px; }
                .footer { background: #f8fafc; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>National Assessment Grid</h1>
                  <p style="margin:4px 0 0 0; opacity:0.8; font-size:13px;">Staff & Administration Portal</p>
                </div>
                <div class="content">
                  <p>Hello <strong>%s</strong>,</p>
                  <p>You have been invited to join the National Assessment Grid (NAG) administrative portal.</p>
                  <div class="roles-box">
                    <strong>Assigned Role(s):</strong> %s
                  </div>
                  <p>To activate your account and configure Multi-Factor Authentication (2FA TOTP), click the button below:</p>
                  <div class="btn-container">
                    <a href="%s" class="btn">Accept Invitation & Setup 2FA</a>
                  </div>
                  <p style="font-size: 13px; color: #64748b;">Or paste this link in your browser:<br>
                    <a href="%s" style="word-break: break-all; color: #2563eb;">%s</a>
                  </p>
                  <p style="font-size: 13px; color: #64748b;">This invitation link is valid for <strong>48 hours</strong>.</p>
                </div>
                <div class="footer">
                  &copy; 2025 National Assessment Grid (NAG). All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                escapeHtml(fullName),
                escapeHtml(roles),
                escapeHtml(inviteLink),
                escapeHtml(inviteLink),
                escapeHtml(inviteLink)
        );

        String textContent = "Hello " + fullName + ",\n\n"
                + "You have been invited to join the National Assessment Grid administrative portal.\n"
                + "Assigned Role(s): " + roles + "\n\n"
                + "Please accept your invitation and configure 2FA by clicking the following link:\n"
                + inviteLink + "\n\n"
                + "This invitation link expires in 48 hours.\n";

        return sendMimeEmail(recipientEmail, subject, htmlContent, textContent);
    }

    private boolean sendMimeEmail(String recipientEmail, String subject, String htmlContent, String textContent) {
        if (mailSender == null) {
            log.info("[EMAIL MOCK] Email to [{}] with subject [{}]", recipientEmail, subject);
            return true;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);

            mailSender.send(mimeMessage);
            log.info("Email successfully sent to [{}] with subject [{}]", recipientEmail, subject);
            return true;
        } catch (Exception e) {
            log.warn("Failed to deliver email to [{}]: {}", recipientEmail, e.getMessage());
            return false;
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
