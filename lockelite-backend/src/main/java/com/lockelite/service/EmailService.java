package com.lockelite.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@lockelite.com}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // ─────────────────────────────────────────────────────────────────
    // 1. OTP VERIFICATION EMAIL
    // ─────────────────────────────────────────────────────────────────
    @Async
    public void sendOtpEmail(String to, String name, String otp) {
        String subject = "LockElite — Your Email Verification Code";
        String html = buildBase(name,
            "Email Verification",
            "#F68222",
            "🔐",
            "Your verification code",
            "Use the OTP below to verify your email address and activate your LockElite account.",
            buildOtpBox(otp, "10 minutes"),
            "<p style='color:#64748b;font-size:14px;margin:0;'>This code expires in <strong>10 minutes</strong>. Do not share this code with anyone — LockElite will never ask for your OTP.</p>",
            null
        );
        sendHtml(to, subject, html);
        log.info("[EMAIL OTP] To: {} | OTP: {}", to, otp);
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. PASSWORD RESET EMAIL
    // ─────────────────────────────────────────────────────────────────
    @Async
    public void sendPasswordResetEmail(String to, String name, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        String subject  = "LockElite — Reset Your Password";
        String html = buildBase(name,
            "Password Reset",
            "#F68222",
            "🔑",
            "Reset your password",
            "We received a request to reset your LockElite account password. Click the button below to set a new password.",
            buildButton("Reset My Password", resetUrl, "#F68222"),
            "<p style='color:#94a3b8;font-size:13px;margin:8px 0 0;text-align:center;'>This link expires in <strong>1 hour</strong>. If you didn't request a password reset, you can safely ignore this email.</p>",
            null
        );
        sendHtml(to, subject, html);
        log.info("[EMAIL] Password reset sent to: {} | Token: {}", to, token);
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. KYC STATUS UPDATE EMAIL
    // ─────────────────────────────────────────────────────────────────
    @Async
    public void sendKycStatusEmail(String to, String name, String status, String reason) {
        boolean approved = status.toUpperCase().contains("APPROVED");
        String color     = approved ? "#10b981" : "#ef4444";
        String icon      = approved ? "✅"       : "❌";
        String headline  = approved ? "KYC Approved — You're all set!"  : "KYC Application Update";
        String subject   = approved ? "LockElite — KYC Verified ✅"     : "LockElite — KYC Status Update";

        String bodyContent = approved
            ? "<p style='color:#374151;font-size:15px;line-height:1.7;margin:0 0 16px;'>Great news! Your KYC application has been <strong style='color:#10b981;'>verified and approved</strong>. You can now explore and book a locker at your nearest branch.</p>"
              + buildButton("Explore Lockers", frontendUrl + "/customer/lockers", "#10b981")
            : "<p style='color:#374151;font-size:15px;line-height:1.7;margin:0 0 16px;'>Your KYC application needs attention. Please review the details below and resubmit.</p>"
              + (reason != null ? "<div style='background:#fef2f2;border-left:4px solid #ef4444;border-radius:8px;padding:16px 20px;margin:16px 0;'><p style='color:#7f1d1d;font-size:14px;margin:0;font-weight:600;'>Reason for rejection:</p><p style='color:#991b1b;font-size:14px;margin:6px 0 0;'>" + reason + "</p></div>" : "")
              + buildButton("Update My KYC", frontendUrl + "/customer/kyc", "#F68222");

        String html = buildBase(name, "KYC Status", color, icon, headline,
            "Here is an update on your Know Your Customer (KYC) verification.",
            bodyContent, null, null);
        sendHtml(to, subject, html);
    }

    // ─────────────────────────────────────────────────────────────────
    // 4. EMPLOYEE WELCOME / CREDENTIALS EMAIL
    // ─────────────────────────────────────────────────────────────────
    @Async
    public void sendEmployeeCredentials(String to, String name, String empCode, String tempPassword) {
        String subject = "LockElite — Welcome to the Team 🎉";
        String credBox = "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:20px 24px;margin:20px 0;'>"
            + "<p style='color:#64748b;font-size:12px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;margin:0 0 12px;'>Your Login Credentials</p>"
            + credRow("Employee Code", empCode)
            + credRow("Email", to)
            + credRow("Temporary Password", "<code style='background:#FFF0E0;color:#F68222;padding:2px 8px;border-radius:6px;font-size:15px;font-weight:700;letter-spacing:1px;'>" + tempPassword + "</code>")
            + "</div>"
            + "<div style='background:#fffbeb;border-left:4px solid #f59e0b;border-radius:8px;padding:14px 18px;margin:16px 0;'>"
            + "<p style='color:#78350f;font-size:13px;margin:0;'>⚠️ <strong>Important:</strong> You will be prompted to change this temporary password on your first login. Please do so immediately.</p>"
            + "</div>"
            + buildButton("Login to LockElite", frontendUrl + "/login", "#F68222");

        String html = buildBase(name, "Employee Welcome", "#F68222", "👔",
            "Welcome aboard, " + name.split(" ")[0] + "!",
            "Your LockElite employee account has been created. Use the credentials below to log in for the first time.",
            credBox, null,
            "<p style='color:#94a3b8;font-size:12px;text-align:center;margin:0;'>This email contains sensitive credentials. Do not forward or share it with anyone.</p>");
        sendHtml(to, subject, html);
        log.info("[EMAIL] Employee credentials sent to: {} | EmpCode: {} | TempPassword: {}", to, empCode, tempPassword);
    }

    // ─────────────────────────────────────────────────────────────────
    // 5. RENT REMINDER EMAIL
    // ─────────────────────────────────────────────────────────────────
    @Async
    public void sendRentReminderEmail(String to, String name, String lockerId, String dueDate, String amount) {
        String subject = "LockElite — Rent Due Reminder for Locker " + lockerId;
        String infoBox = "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:20px 24px;margin:20px 0;'>"
            + "<p style='color:#64748b;font-size:12px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;margin:0 0 12px;'>Payment Details</p>"
            + credRow("Locker Number", lockerId)
            + credRow("Due Date",      dueDate)
            + credRow("Amount Due",    "₹" + amount)
            + "</div>"
            + "<div style='background:#fef3c7;border-left:4px solid #f59e0b;border-radius:8px;padding:14px 18px;margin:16px 0;'>"
            + "<p style='color:#78350f;font-size:13px;margin:0;'>⏰ A late payment penalty of <strong>₹50 per day</strong> will apply after the due date.</p>"
            + "</div>"
            + buildButton("Pay Rent Now", frontendUrl + "/customer/bookings", "#F68222");

        String html = buildBase(name, "Rent Reminder", "#f59e0b", "💳",
            "Your locker rent is due soon",
            "This is a friendly reminder that your locker rent payment is due. Please make the payment before the due date to avoid penalties.",
            infoBox, null, null);
        sendHtml(to, subject, html);
    }

    // ─────────────────────────────────────────────────────────────────
    // 6. ★ DIGITAL ACCESS KEY EMAIL (30 min before visit)
    // ─────────────────────────────────────────────────────────────────
    @Async
    public void sendDigitalAccessKey(String to, String name, String digitalKey,
                                     String lockerNumber, String visitDate, String visitTime,
                                     String branchName, String branchAddress,
                                     String bankName, String purpose) {

        String subject = "LockElite — Your Digital Locker Key for Today's Visit 🔐";

        // Break key into groups for readability: LK-A3F9B2
        String displayKey = digitalKey;

        String keyBox = "<div style='text-align:center;margin:28px 0;'>"
            + "<p style='color:#64748b;font-size:12px;font-weight:700;letter-spacing:.1em;text-transform:uppercase;margin:0 0 12px;'>Your Digital Access Key</p>"
            + "<div style='display:inline-block;background:linear-gradient(135deg,#1e293b 0%,#0f172a 100%);border-radius:16px;padding:24px 40px;box-shadow:0 8px 30px rgba(0,0,0,0.15);'>"
            + "<p style='font-family:\"Courier New\",monospace;font-size:36px;font-weight:900;letter-spacing:8px;color:#F68222;margin:0;text-shadow:0 0 20px rgba(246,130,34,0.4);'>"
            + displayKey
            + "</p>"
            + "<p style='color:rgba(255,255,255,0.4);font-size:11px;margin:10px 0 0;letter-spacing:.05em;'>SHOW THIS TO THE LOCKER OFFICER AT ENTRY</p>"
            + "</div>"
            + "</div>";

        String detailsBox = "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:20px 24px;margin:20px 0;'>"
            + "<p style='color:#64748b;font-size:12px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;margin:0 0 14px;'>Visit Details</p>"
            + credRow("📅 Date",         visitDate)
            + credRow("⏰ Time",         visitTime)
            + credRow("🔐 Locker",       lockerNumber)
            + credRow("🏦 Branch",       branchName)
            + credRow("📍 Address",      branchAddress)
            + credRow("📋 Purpose",      purpose)
            + credRow("🏛️ Bank",        bankName)
            + "</div>";

        String warningBox = "<div style='background:#fef2f2;border-left:4px solid #ef4444;border-radius:8px;padding:16px 20px;margin:16px 0;'>"
            + "<p style='color:#7f1d1d;font-size:13px;font-weight:700;margin:0 0 6px;'>⚠️ Security Notice</p>"
            + "<ul style='color:#991b1b;font-size:13px;margin:0;padding-left:18px;line-height:1.7;'>"
            + "<li>This key is valid for <strong>your visit window only</strong></li>"
            + "<li>Present this key to the locker officer at the branch entry</li>"
            + "<li>Do not share this key with anyone</li>"
            + "<li>Key expires automatically after your visit</li>"
            + "<li>Contact branch immediately if you did not schedule this visit</li>"
            + "</ul>"
            + "</div>";

        String stepsBox = "<div style='margin:24px 0;'>"
            + "<p style='color:#374151;font-size:13px;font-weight:700;letter-spacing:.05em;text-transform:uppercase;margin:0 0 14px;'>How to use your digital key</p>"
            + step("01", "Arrive at " + branchName + " at your scheduled time")
            + step("02", "Show this email (or the key code) to the locker officer")
            + step("03", "Officer verifies the key in the system")
            + step("04", "Access your locker " + lockerNumber + " is granted")
            + "</div>";

        String footer = "<p style='color:#94a3b8;font-size:12px;text-align:center;margin:0;'>This is an automated security email. If you did not schedule a visit, please <a href='" + frontendUrl + "/customer/bookings' style='color:#F68222;'>contact your branch</a> immediately.</p>";

        String html = buildBase(name, "Digital Access Key", "#F68222", "🔐",
            "Your locker visit is in 30 minutes!",
            "Your scheduled visit to " + branchName + " is coming up. Here is your one-time digital access key to enter the locker area.",
            keyBox + detailsBox + stepsBox + warningBox,
            null, footer);

        sendHtml(to, subject, html);
        log.info("[EMAIL] Digital access key sent to: {} | Key: {} | Locker: {}", to, digitalKey, lockerNumber);
    }

    // ─────────────────────────────────────────────────────────────────
    // 7. ALLOCATION APPROVED EMAIL
    // ─────────────────────────────────────────────────────────────────
    @Async
    public void sendAllocationApprovedEmail(String to, String name, String lockerNumber, String branch, String rent) {
        String subject = "LockElite — Your Locker Has Been Allocated 🎉";
        String infoBox = "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-radius:12px;padding:20px 24px;margin:20px 0;'>"
            + "<p style='color:#14532d;font-size:12px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;margin:0 0 12px;'>Allocation Details</p>"
            + credRow("Locker Number", "<strong style='color:#10b981;font-size:16px;'>" + lockerNumber + "</strong>")
            + credRow("Branch",        branch)
            + credRow("Annual Rent",   "₹" + rent)
            + "</div>"
            + buildButton("Book a Visit", frontendUrl + "/customer/bookings", "#10b981");

        String html = buildBase(name, "Locker Allocated", "#10b981", "🎉",
            "Your locker is ready!",
            "Your locker allocation request has been approved by the branch officers. Your locker is now active and ready for use.",
            infoBox, null, null);
        sendHtml(to, subject, html);
    }

    // ═════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS — HTML email builder
    // ═════════════════════════════════════════════════════════════════

    private String buildBase(String name, String badge, String accentColor, String icon,
                              String headline, String subheadline,
                              String bodyContent, String belowBody, String footerExtra) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'/>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1.0'/>"
            + "<title>" + headline + "</title></head>"
            + "<body style='margin:0;padding:0;background:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,\"Helvetica Neue\",Arial,sans-serif;'>"
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background:#f1f5f9;min-height:100vh;'><tr><td align='center' style='padding:40px 16px;'>"
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='max-width:600px;'>"

            // ── Header bar ───────────────────────────────────────────
            + "<tr><td style='background:#0f172a;border-radius:16px 16px 0 0;padding:20px 32px;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0'><tr>"
            + "<td><div style='display:inline-flex;align-items:center;gap:10px;'>"
            + "<div style='background:" + accentColor + ";border-radius:10px;width:36px;height:36px;display:inline-flex;align-items:center;justify-content:center;font-weight:900;color:white;font-size:13px;line-height:36px;text-align:center;'>LE</div>"
            + "<div><p style='color:white;font-size:14px;font-weight:800;letter-spacing:.12em;margin:0;line-height:1;'>LOCKELITE</p>"
            + "<p style='color:rgba(255,255,255,0.35);font-size:10px;margin:3px 0 0;letter-spacing:.06em;'>BANK LOCKER PLATFORM</p></div>"
            + "</div></td>"
            + "<td align='right'><span style='background:" + accentColor + "22;color:" + accentColor + ";font-size:11px;font-weight:700;padding:4px 10px;border-radius:20px;letter-spacing:.06em;'>" + badge.toUpperCase() + "</span></td>"
            + "</tr></table></td></tr>"

            // ── Hero icon ─────────────────────────────────────────────
            + "<tr><td style='background:white;padding:40px 32px 8px;text-align:center;'>"
            + "<div style='width:72px;height:72px;border-radius:20px;background:" + accentColor + "18;display:inline-flex;align-items:center;justify-content:center;margin:0 auto 20px;font-size:36px;line-height:72px;text-align:center;'>" + icon + "</div>"
            + "<h1 style='color:#0f172a;font-size:24px;font-weight:800;margin:0 0 10px;line-height:1.3;'>" + headline + "</h1>"
            + "<p style='color:#64748b;font-size:15px;line-height:1.6;margin:0 0 28px;'>" + subheadline + "</p>"
            + "<div style='height:1px;background:#f1f5f9;margin:0 -32px 28px;'></div>"
            + "</td></tr>"

            // ── Greeting ──────────────────────────────────────────────
            + "<tr><td style='background:white;padding:0 32px;'>"
            + "<p style='color:#374151;font-size:15px;line-height:1.7;margin:0 0 20px;'>Dear <strong>" + name + "</strong>,</p>"
            + bodyContent
            + (belowBody != null ? belowBody : "")
            + "</td></tr>"

            // ── Footer ────────────────────────────────────────────────
            + "<tr><td style='background:white;border-radius:0 0 16px 16px;padding:28px 32px;border-top:1px solid #f1f5f9;'>"
            + "<p style='color:#374151;font-size:14px;margin:0 0 4px;'>Warm regards,</p>"
            + "<p style='color:#0f172a;font-size:15px;font-weight:700;margin:0 0 20px;'>The LockElite Team</p>"
            + "<div style='height:1px;background:#f1f5f9;margin:0 0 20px;'></div>"
            + (footerExtra != null ? footerExtra + "<div style='height:16px;'></div>" : "")
            + "<p style='color:#94a3b8;font-size:11px;margin:0;line-height:1.6;text-align:center;'>"
            + "© 2026 LockElite — Bank Locker Administration Platform<br/>"
            + "This email was sent to you because you have an account with LockElite.<br/>"
            + "<a href='" + frontendUrl + "' style='color:#F68222;text-decoration:none;'>Visit platform</a>"
            + "</p></td></tr>"

            + "</table></td></tr></table></body></html>";
    }

    private String buildOtpBox(String otp, String validity) {
        // Split OTP into individual digit boxes
        StringBuilder digits = new StringBuilder();
        for (char c : otp.toCharArray()) {
            digits.append("<span style='display:inline-block;width:44px;height:52px;line-height:52px;text-align:center;"
                + "background:#f8fafc;border:2px solid #e2e8f0;border-radius:10px;font-size:26px;font-weight:900;"
                + "color:#0f172a;margin:0 4px;font-family:\"Courier New\",monospace;'>").append(c).append("</span>");
        }
        return "<div style='text-align:center;margin:28px 0;'>"
            + "<p style='color:#64748b;font-size:12px;font-weight:700;letter-spacing:.1em;text-transform:uppercase;margin:0 0 16px;'>One-Time Password</p>"
            + "<div style='margin:0 0 16px;'>" + digits + "</div>"
            + "<p style='color:#94a3b8;font-size:13px;margin:0;'>Valid for <strong style='color:#374151;'>" + validity + "</strong></p>"
            + "</div>";
    }

    private String buildButton(String label, String url, String color) {
        return "<div style='text-align:center;margin:28px 0;'>"
            + "<a href='" + url + "' style='display:inline-block;background:" + color + ";color:white;"
            + "padding:14px 36px;border-radius:12px;font-size:15px;font-weight:700;text-decoration:none;"
            + "letter-spacing:.02em;box-shadow:0 4px 14px " + color + "44;'>" + label + " →</a>"
            + "</div>";
    }

    private String credRow(String label, String value) {
        return "<div style='display:flex;justify-content:space-between;align-items:center;padding:10px 0;"
            + "border-bottom:1px solid #f1f5f9;'>"
            + "<span style='color:#64748b;font-size:13px;'>" + label + "</span>"
            + "<span style='color:#0f172a;font-size:14px;font-weight:600;'>" + value + "</span>"
            + "</div>";
    }

    private String step(String num, String text) {
        return "<div style='display:flex;align-items:flex-start;gap:14px;margin:0 0 12px;padding:14px 16px;"
            + "background:#f8fafc;border-radius:10px;'>"
            + "<div style='min-width:28px;height:28px;background:#F68222;border-radius:50%;line-height:28px;"
            + "text-align:center;color:white;font-size:12px;font-weight:900;'>" + num + "</div>"
            + "<p style='color:#374151;font-size:14px;margin:4px 0 0;line-height:1.5;'>" + text + "</p>"
            + "</div>";
    }

    // ─────────────────────────────────────────────────────────────────
    // SEND HTML EMAIL (with fallback to log)
    // ─────────────────────────────────────────────────────────────────
    private void sendHtml(String to, String subject, String htmlBody) {
        if (mailSender == null) {
            log.info("[EMAIL MOCK] To: {} | Subject: {}", to, subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "LockElite Platform");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);   // true = isHtml
            mailSender.send(message);
            log.info("[EMAIL] Sent to: {} | Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to {}: {}", to, e.getMessage());
        }
    }
}
