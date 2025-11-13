import java.math.BigDecimal;
import java.sql.*;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class BalanceAlert {

    // Minimum balance threshold
    private static final BigDecimal MIN_BALANCE = new BigDecimal("500.00");

    public static void checkLowBalance(Connection conn, int accountNumber) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT email, balance, holder_name FROM accounts WHERE account_number=?"
            );
            ps.setInt(1, accountNumber);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                BigDecimal balance = rs.getBigDecimal("balance");
                String email = rs.getString("email");
                String name = rs.getString("holder_name");

                // Trigger alert if balance is below or near minimum threshold
                if (balance.compareTo(MIN_BALANCE) <= 0) {
                    sendLowBalanceEmail(email, name, balance);
                    System.out.println("⚠ Low balance alert sent to " + email);
                }
            }
        } catch (Exception e) {
            System.out.println("Error checking low balance: " + e.getMessage());
        }
    }

    private static void sendLowBalanceEmail(String email, String name, BigDecimal balance) {
        final String senderEmail = "gadilasowmya147@gmail.com";
        final String senderPassword = "pkay chfx qyst gnvy";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Low Balance Alert - SecureBank");

            message.setText(
                    "Dear " + name + ",\n\n" +
                            "This is a gentle reminder that your account balance has dropped to ₹" + balance + ".\n" +
                            "Please ensure sufficient funds are maintained to avoid penalties or transaction failures.\n\n" +
                            "Minimum Required Balance: ₹" + MIN_BALANCE + "\n\n" +
                            "Regards,\n" +
                            "SecureBank Support Team"
            );
            Transport.send(message);
            System.out.println("📩 Low balance alert email sent to: " + email);

        } catch (Exception e) {
            System.out.println("⚠️ Failed to send low balance email: " + e.getMessage());
        }

    }
}
