import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class TransferEmailService {

    private static final String SENDER_EMAIL = "yourbank@gmail.com"; // replace with your Gmail
    private static final String SENDER_PASSWORD = "your-app-password"; // replace with Gmail App Password

    public static void sendTransferReceipt(
            String email,
            String name,
            String otherParty,
            double amount,
            double balance,
            String type
    ) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));

            if (type.equalsIgnoreCase("SENT")) {
                message.setSubject("SecureBank - Transfer Confirmation");
                message.setText(
                        "Dear " + name + ",\n\n" +
                                "Your transfer of ₹" + amount + " to " + otherParty + " has been successfully processed.\n" +
                                "Your current account balance is ₹" + balance + ".\n\n" +
                                "Transaction Time: " + java.time.LocalDateTime.now() + "\n\n" +
                                "Thank you for banking with us!\n\n" +
                                "Regards,\nSecureBank Support Team"
                );
            } else {
                message.setSubject("SecureBank - Amount Credited");
                message.setText(
                        "Dear " + name + ",\n\n" +
                                "₹" + amount + " has been credited to your account by " + otherParty + ".\n" +
                                "Transaction Time: " + java.time.LocalDateTime.now() + "\n\n" +
                                "Thank you for banking with us!\n\n" +
                                "Regards,\nSecureBank Support Team"
                );
            }

            Transport.send(message);
            System.out.println("📧 Email (" + type + ") sent successfully to " + email);

        } catch (Exception e) {
            System.out.println("⚠ Failed to send transfer email: " + e.getMessage());
        }
    }
}
