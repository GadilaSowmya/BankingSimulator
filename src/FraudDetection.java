import java.math.BigDecimal;
import java.sql.*;
import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class FraudDetection {

    private static final String SENDER_EMAIL = "gadilasowmya147@gmail.com";  // your Gmail
    private static final String SENDER_PASSWORD = "pkay chfx qyst gnvy";     // your app password

    public static void checkFraud(Connection conn, int loggedInAccount, BigDecimal amount) {
        try {
            boolean suspicious = false;
            String reason = "";

            //  Check high-value transactions
            if (amount.compareTo(new BigDecimal("15000")) > 0) {
                suspicious = true;
                reason = "High-value transaction detected (₹" + amount + ")";
            }

            //  Check frequency (more than 5 transfers in 1 minute)
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM transactions WHERE from_account = ? " +
                            "AND type IN ('TRANSFER_SENT', 'TRANSFER_RECEIVED') " +
                            "AND created_at > NOW() - INTERVAL 1 MINUTE"
            );
            ps.setInt(1, loggedInAccount);
            ResultSet rs = ps.executeQuery();

            if (rs.next() && rs.getInt(1) >= 5) {
                suspicious = true;
                reason = "Multiple transfers (5+) within 1 minute.";
            }

            //  If suspicious, insert record & send email alert
            if (suspicious) {
                PreparedStatement alertPs = conn.prepareStatement(
                        "INSERT INTO fraud_alerts(account_number, reason, timestamp) VALUES (?, ?, NOW())"
                );
                alertPs.setInt(1, loggedInAccount);
                alertPs.setString(2, reason);
                alertPs.executeUpdate();

                // Send alert email
                sendFraudAlertEmail(conn, loggedInAccount, reason);
                System.out.println("🚨 FRAUD ALERT: " + reason);
            }

        } catch (Exception e) {
            System.out.println("⚠ Error checking fraud: " + e.getMessage());
        }
    }

    // Send fraud alert email
    private static void sendFraudAlertEmail(Connection conn, int accNo, String reason) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT email, holder_name FROM accounts WHERE account_number=?");
            ps.setInt(1, accNo);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return; // no email found

            String recipientEmail = rs.getString("email");
            String holderName = rs.getString("holder_name");

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(" Fraud Alert: Unusual Activity Detected");
            message.setText(
                    "Dear " + holderName + ",\n\n" +
                            "We detected unusual activity on your account (A/C No: " + accNo + ").\n\n" +
                            "Reason: " + reason + "\n" +
                            "If this wasn't you, please contact your bank immediately.\n\n" +
                            "— SecureBank Automated Alert System"
            );

            Transport.send(message);
            System.out.println("📧 Fraud alert email sent to " + recipientEmail);

        } catch (Exception e) {
            System.out.println("⚠ Failed to send fraud alert email: " + e.getMessage());
        }
    }
}
