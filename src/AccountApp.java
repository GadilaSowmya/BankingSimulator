
import java.math.BigDecimal;
import java.sql.*;
import java.util.Scanner;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;
import jakarta.mail.internet.*;
import java.util.Properties;
import java.util.Random;
import java.io.File;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;


public class AccountApp {
    private static final Scanner sc = new Scanner(System.in);
    private static Integer loggedInAccount = null; // Store logged-in user's account number


    public static void main(String[] args) {
        try (Connection conn = MyJDBC.getConnection()) {
            while (true) {
                System.out.println("\n===== BANKING SYSTEM =====");
                System.out.println("1 Create Account");
                System.out.println("2 Login as User");
                System.out.println("3 Login as Admin");
                System.out.println("4 Forgot PIN");

                System.out.println("5 Exit");
                System.out.print("Choose an option: ");
                int choice = sc.nextInt();

                switch (choice) {
                    case 1 -> createAccount(conn);
                    case 2 -> login(conn);
                    case 3 -> adminLogin(conn);
                    case 4 -> forgotPin(conn);
                    case 5 -> {
                        System.out.println(" Thank you for using our bank!");
                        return;
                    }
                    default -> System.out.println(" Invalid choice. Try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createAccount(Connection conn) throws SQLException {
        sc.nextLine(); // clear buffer

        String name, email, pin;
        BigDecimal balance;

        // ---- NAME INPUT LOOP ----
        while (true) {
            System.out.print("Enter Name: ");
            name = sc.nextLine().trim();

            if (name.matches("^[A-Za-z]+( [A-Za-z]+)*$")) break;

            System.out.println(" Invalid name! Only alphabets allowed.");
            if (!retryPrompt()) return;
        }

        // ---- EMAIL INPUT LOOP + OTP VERIFY ----
        while (true) {
            System.out.print("Enter Email: ");
            email = sc.next();

            if (!email.matches("^[a-z0-9+_.-]+@[a-z0-9.-]+$")) {
                System.out.println(" Invalid email format!");
                if (!retryPrompt()) return;
                continue;
            }

            PreparedStatement checkEmail = conn.prepareStatement("SELECT * FROM accounts WHERE email = ?");
            checkEmail.setString(1, email);
            ResultSet rs = checkEmail.executeQuery();

            if (rs.next()) {
                System.out.println(" Account with this email already exists!");
                if (!retryPrompt()) return;
                continue;
            }

            // ---- Send OTP ----
            System.out.println(" Sending OTP to " + email + "...");
            String otp = sendOTP(email);

            if (otp == null || otp.isEmpty()) {
                System.out.println(" Failed to send OTP. Please try again with a valid email.");
                if (!retryPrompt()) return;
                continue;
            }

            System.out.print("Enter OTP received in email: ");
            String userOtp = sc.next();

            if (!userOtp.equals(otp)) {
                System.out.println("  Incorrect OTP! Email verification failed.");
                if (!retryPrompt()) return;
                continue;
            }

            System.out.println("  Email verified successfully!");
            break;
        }

        // ---- PIN INPUT LOOP ----
        while (true) {
            System.out.print("Set 4-digit PIN: ");
            pin = sc.next();

            if (pin.matches("\\d{4}")) break;

            System.out.println(" PIN must be exactly 4 digits!");
            if (!retryPrompt()) return;
        }

        // ---- INITIAL BALANCE LOOP ----
        while (true) {
            System.out.print("Enter Initial Deposit (Min 500/-): ");

            if (!sc.hasNextBigDecimal()) {
                System.out.println(" Enter numbers only!");
                sc.next();
                if (!retryPrompt()) return;
                continue;
            }

            balance = sc.nextBigDecimal();

            if (balance.compareTo(new BigDecimal("500")) >= 0) break;

            System.out.println(" Minimum initial deposit is 500!");
            if (!retryPrompt()) return;
        }

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO accounts(holder_name, email, pin, balance) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
        );
        ps.setString(1, name);
        ps.setString(2, email);
        ps.setString(3, pin);
        ps.setBigDecimal(4, balance);
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) {
            System.out.println("\n🎉 Account Created Successfully!");
            System.out.println(" Your Account Number: " + keys.getInt(1));
        }
    }


    private static boolean retryPrompt() {
        System.out.println("\n1 Try Again");
        System.out.println("2 Exit to Main Menu");
        System.out.print("Choose: ");

        if (!sc.hasNextInt()) { sc.next(); return false; }

        int ch = sc.nextInt();
        sc.nextLine();

        return ch == 1;
    }


    private static String sendOTP(String email) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000));

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
            message.setSubject("Bank Login OTP Verification");
            message.setText("Your OTP for Login: " + otp);

            Transport.send(message);
            System.out.println(" OTP sent to your email!");
        } catch (Exception e) {
            System.out.println(" Failed to send OTP: " + e.getMessage());
        }
        return otp;
    }




    private static void sendStatementByEmail(String toEmail, String filePath) {
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
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Your Bank Account Statement");

            // Email body
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("Dear Customer,\n\nPlease find attached your latest bank account statement.\n\nRegards,\nBank Support");

            // Attachment
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(new java.io.File(filePath));

            // Combine both parts
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);

            Transport.send(message);
            System.out.println(" Statement sent successfully to: " + toEmail);

        } catch (Exception e) {
            System.out.println(" Failed to send email: " + e.getMessage());
        }
    }

    private static void login(Connection conn) throws SQLException {
        while (true) {
            System.out.print("Enter Account Number: ");
            int accNo = sc.nextInt();
            System.out.print("Enter PIN: ");
            String pin = sc.next();

            PreparedStatement ps = conn.prepareStatement("SELECT * FROM accounts WHERE account_number=? AND pin=?");
            ps.setInt(1, accNo);
            ps.setString(2, pin);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                loggedInAccount = accNo;
                String email = rs.getString("email");
                String otp = sendOTP(email);

                System.out.print("Enter OTP sent to email: ");
                String userOtp = sc.next();

                if (!otp.equals(userOtp)) {
                    System.out.println(" Incorrect OTP! Login failed.");
                    return;
                }

                System.out.println("\n Login successful! Welcome, " + rs.getString("holder_name") + "!");
                loggedInAccount = accNo;
                accountMenu(conn);
                return;

            } else {
                System.out.println("\n Invalid Account Number or PIN!");

                System.out.println("\n1. Try Again");
                System.out.println("2️. Back to Main Menu");
                System.out.print("Choose an option: ");
                int choice = sc.nextInt();

                if (choice == 2) {
                    return; // go back to main menu
                } else if (choice != 1) {
                    System.out.println("⚠ Invalid choice. Returning to Main Menu...");
                    return;
                }
            }
        }
    }


    private static void forgotPin(Connection conn) throws SQLException {
        sc.nextLine();
        System.out.print("Enter your registered email: ");
        String email = sc.nextLine().trim();

        // Check if email exists
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM accounts WHERE email=?");
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            System.out.println(" No account found with this email!");
            return;
        }

        System.out.println(" Account found! Sending OTP to " + email);
        String otp = sendOTP(email);

        if (otp == null) {
            System.out.println(" Failed to send OTP. Please try again later.");
            return;
        }

        System.out.print("Enter OTP received in your email: ");
        String userOtp = sc.nextLine().trim();

        if (!otp.equals(userOtp)) {
            System.out.println(" Incorrect OTP! PIN reset failed.");
            return;
        }

        // OTP verified → set new PIN
        String newPin;
        while (true) {
            System.out.print("Enter new 4-digit PIN: ");
            newPin = sc.nextLine().trim();

            if (newPin.matches("\\d{4}")) break;
            System.out.println(" PIN must be exactly 4 digits!");
        }

        PreparedStatement update = conn.prepareStatement("UPDATE accounts SET pin=? WHERE email=?");
        update.setString(1, newPin);
        update.setString(2, email);
        update.executeUpdate();

        System.out.println(" Your PIN has been reset successfully!");
        // ---- Send confirmation email ----
        try {
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

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("PIN Reset Confirmation - SecureBank");
            message.setText("""
            Dear Customer,
            
            Your account PIN has been successfully reset.
            
            If you did not request this change, please contact our support team immediately.
            
            Thank you,
            SecureBank Security Team
            """);

            Transport.send(message);
            System.out.println(" Confirmation email sent to " + email);
        } catch (Exception e) {
            System.out.println(" Failed to send confirmation email: " + e.getMessage());
        }

    }


    private static void adminLogin(Connection conn) throws SQLException {
        System.out.print("Enter Admin Username: ");
        String username = sc.next();
        System.out.print("Enter Admin Password: ");
        String password = sc.next();

        PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM admin WHERE username=? AND password=?");
        ps.setString(1, username);
        ps.setString(2, password);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            System.out.println("\n Admin Login Successful!");
            adminMenu(conn);
        } else {
            System.out.println(" Invalid Admin Credentials!");
        }
    }


    private static void adminMenu(Connection conn) throws SQLException {
        while (true) {
            System.out.println("\n===== ADMIN PANEL =====");
            System.out.println("1 View All Accounts");
            System.out.println("2 Search Account");
            System.out.println("3 Delete Account");
            System.out.println("4 View All Transactions");
            System.out.println("5 Logout");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();

            switch (choice) {
                case 1 -> viewAllAccounts(conn);
                case 2 -> searchAccount(conn);
                case 3 -> deleteAccount(conn);
                case 4 -> viewAllTransactions(conn);
                case 5 -> { return; }
                default -> System.out.println(" Invalid choice.");
            }
        }
    }


    private static void viewAllAccounts(Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM accounts");
        ResultSet rs = ps.executeQuery();

        System.out.printf("%-10s %-20s %-25s %-10s\n", "Acc No", "Name", "Email", "Balance");
        while (rs.next()) {
            System.out.printf("%-10s %-20s %-25s %-10s\n",
                    rs.getInt("account_number"),
                    rs.getString("holder_name"),
                    rs.getString("email"),
                    rs.getBigDecimal("balance"));
        }
    }

    private static void searchAccount(Connection conn) throws SQLException {
        System.out.print("Enter Account Number: ");
        int acc = sc.nextInt();

        PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM accounts WHERE account_number=?");
        ps.setInt(1, acc);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            System.out.println("Account Number: " + rs.getInt("account_number"));
            System.out.println("Holder Name: " + rs.getString("holder_name"));
            System.out.println("Email: " + rs.getString("email"));
            System.out.println("Balance: " + rs.getBigDecimal("balance"));
        } else {
            System.out.println(" Account Not Found!");
        }
    }

    private static void deleteAccount(Connection conn) throws SQLException {
        System.out.print("Enter Account Number to Delete: ");
        int acc = sc.nextInt();

        // Delete all related transactions first
        PreparedStatement delTrans = conn.prepareStatement(
                "DELETE FROM transactions WHERE from_account=? OR to_account=?"
        );
        delTrans.setInt(1, acc);
        delTrans.setInt(2, acc);
        delTrans.executeUpdate();

        // Then delete the account
        PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM accounts WHERE account_number=?"
        );
        ps.setInt(1, acc);
        int rows = ps.executeUpdate();

        if (rows > 0) {
            System.out.println(" Account and related transactions deleted successfully!");
        } else {
            System.out.println(" Account Not Found!");
        }
    }

    private static void viewAllTransactions(Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM transactions ORDER BY created_at DESC");
        ResultSet rs = ps.executeQuery();

        System.out.println("\n===== ALL TRANSACTIONS =====");
        System.out.printf("%-5s %-12s %-12s %-20s %-15s %-20s\n",
                "ID", "From", "To", "Type", "Amount", "Time");

        while (rs.next()) {
            System.out.printf("%-5d %-12s %-12s %-10s %-10s %-20s\n",
                    rs.getInt("id"),
                    rs.getString("from_account"),
                    rs.getString("to_account") == null ? "-" : rs.getString("to_account"),
                    rs.getString("type"),
                    rs.getBigDecimal("amount"),
                    rs.getTimestamp("created_at"));
        }
    }


    private static void accountMenu(Connection conn) throws SQLException {
        while (true) {
            System.out.println("\n===== ACCOUNT MENU =====");
            System.out.println("1️ Deposit");
            System.out.println("2️ Withdraw");
            System.out.println("3️ Transfer");
            System.out.println("4️ View Balance");
            System.out.println("5 View Transactions");
            System.out.println("6 View Account Details");
            System.out.println("7 Download Statement");

            System.out.println("8 Logout");

            System.out.print("Choose an option: ");
            int choice = sc.nextInt();

            switch (choice) {
                case 1 -> deposit(conn);
                case 2 -> withdraw(conn);
                case 3 -> transfer(conn);
                case 4 -> viewBalance(conn, loggedInAccount);
                case 5 -> viewTransactions(conn);
                case 6 -> viewAccountDetails(conn);
                case 7 -> downloadStatement(conn);

                case 8 -> {
                    System.out.println(" Logged out successfully!");
                    loggedInAccount = null;
                    return;
                }

                default -> System.out.println(" Invalid choice.");
            }
        }
    }

    private static void viewAccountDetails(Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT account_number, holder_name, email, balance FROM accounts WHERE account_number=?"
        );
        ps.setInt(1, loggedInAccount);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            System.out.println("\n===== ACCOUNT DETAILS =====");
            System.out.println("Account Number : " + rs.getInt("account_number"));
            System.out.println("Account Holder : " + rs.getString("holder_name"));
            System.out.println("Email          : " + rs.getString("email"));
            System.out.println("Balance        : " + rs.getBigDecimal("balance"));
        } else {
            System.out.println("Account details not found!");
        }
    }

    private static void deposit(Connection conn) throws SQLException {
        BigDecimal amount = inputAmountWithRetry("deposit");
        if (amount.compareTo(BigDecimal.ZERO) == 0) return;

        PreparedStatement ps = conn.prepareStatement(
                "UPDATE accounts SET balance = balance + ? WHERE account_number=?"
        );
        ps.setBigDecimal(1, amount);
        ps.setInt(2, loggedInAccount);
        ps.executeUpdate();

        // Log Transaction (CREDIT)
        PreparedStatement log = conn.prepareStatement(
                "INSERT INTO transactions(from_account, to_account, amount, type) VALUES (?, ?, ?, ?)"
        );
        log.setNull(1, loggedInAccount);
        log.setInt(2, loggedInAccount);
        log.setBigDecimal(3, amount);
        log.setString(4, "DEPOSIT");
        log.executeUpdate();

        System.out.println(" Deposit successful!");
        viewBalance(conn, loggedInAccount);
    }


    private static void withdraw(Connection conn) throws SQLException {
        BigDecimal amount = inputAmountWithRetry("withdraw");
        if (amount.compareTo(BigDecimal.ZERO) == 0) return;

        PreparedStatement ps = conn.prepareStatement("SELECT balance FROM accounts WHERE account_number=?");
        ps.setInt(1, loggedInAccount);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            BigDecimal balance = rs.getBigDecimal("balance");

            if (balance.compareTo(amount) >= 0) {
                PreparedStatement update = conn.prepareStatement(
                        "UPDATE accounts SET balance = balance - ? WHERE account_number=?"
                );
                update.setBigDecimal(1, amount);
                update.setInt(2, loggedInAccount);
                update.executeUpdate();

                // Log Transaction (DEBIT)
                PreparedStatement log = conn.prepareStatement(
                        "INSERT INTO transactions(from_account, to_account, amount, type) VALUES (?, ?, ?, ?)"
                );
                log.setInt(1, loggedInAccount);
                log.setNull(2, loggedInAccount);
                log.setBigDecimal(3, amount);
                log.setString(4, "WITHDRAW");
                log.executeUpdate();

                System.out.println(" Withdrawal successful!");
                viewBalance(conn, loggedInAccount);
                BalanceAlert.checkLowBalance(conn, loggedInAccount);

            } else {
                System.out.println(" Insufficient funds!");
            }
        }
    }


    private static void transfer(Connection conn) throws SQLException {
        System.out.print("Enter recipient account number: ");
        int toAccount = sc.nextInt();

        System.out.print("Enter amount to transfer: ");
        BigDecimal amount = sc.nextBigDecimal();

        // Step 1: Verify sender
        PreparedStatement psSender = conn.prepareStatement(
                "SELECT holder_name, email, balance FROM accounts WHERE account_number = ?"
        );
        psSender.setInt(1, loggedInAccount);
        ResultSet rsSender = psSender.executeQuery();

        if (!rsSender.next()) {
            System.out.println("⚠ Invalid sender account!");
            return;
        }
        String senderName = rsSender.getString("holder_name");
        String senderEmail = rsSender.getString("email");
        BigDecimal senderBalance = rsSender.getBigDecimal("balance");

        if (senderBalance.compareTo(amount) < 0) {
            System.out.println(" Insufficient funds!");
            return;
        }
        // Step 2: Verify recipient
        PreparedStatement psReceiver = conn.prepareStatement(
                "SELECT holder_name FROM accounts WHERE account_number = ?"
        );
        psReceiver.setInt(1, toAccount);
        ResultSet rsReceiver = psReceiver.executeQuery();

        if (!rsReceiver.next()) {
            System.out.println("⚠ Recipient account not found!");
            return;
        }

        String receiverName = rsReceiver.getString("holder_name");

        // Step 3: Ask for transfer confirmation
        sc.nextLine(); // clear buffer
        System.out.print("Confirm transfer of ₹" + amount + " to " + receiverName + " (yes/no): ");
        String confirm = sc.nextLine().trim().toLowerCase();

        if (!confirm.equals("yes")) {
            System.out.println(" Transaction cancelled by user.");
            return;
        }

        // Step 4: OTP generation
        String otp = sendOTP(senderEmail);
        long otpSentTime = System.currentTimeMillis();

        System.out.print("Enter OTP sent to your email (valid for 2 minutes): ");
        String enteredOtp = sc.nextLine().trim();
        long otpEnteredTime = System.currentTimeMillis();

        // Step 5: OTP validation
        long timeDiff = otpEnteredTime - otpSentTime;
        if (timeDiff >= 120000) { // 2 minutes = 120,000 ms
            System.out.println(" OTP expired! Please try again.");
            return;
        }

        if (!enteredOtp.equals(otp)) {
            System.out.println(" Incorrect OTP! Transaction cancelled.");
            return;
        }

        // Step 6: Proceed with transfer
        conn.setAutoCommit(false);
        try {
            PreparedStatement debit = conn.prepareStatement(
                    "UPDATE accounts SET balance = balance - ? WHERE account_number = ?"
            );
            debit.setBigDecimal(1, amount);
            debit.setInt(2, loggedInAccount);
            debit.executeUpdate();

            PreparedStatement credit = conn.prepareStatement(
                    "UPDATE accounts SET balance = balance + ? WHERE account_number = ?"
            );
            credit.setBigDecimal(1, amount);
            credit.setInt(2, toAccount);
            credit.executeUpdate();

            PreparedStatement log = conn.prepareStatement(
                    "INSERT INTO transactions(from_account, to_account, amount, type) VALUES (?, ?, ?, 'transfer')"
            );
            log.setInt(1, loggedInAccount);
            log.setInt(2, toAccount);
            log.setBigDecimal(3, amount);
            log.executeUpdate();

            conn.commit();

            System.out.println(" Transaction successful!");
            System.out.println(" ₹" + amount + " transferred to " + receiverName);


        } catch (Exception e) {
            conn.rollback();
            System.out.println(" Transaction failed: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }



    private static void viewBalance(Connection conn, int accNo) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT balance FROM accounts WHERE account_number=?");
        ps.setInt(1, accNo);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            System.out.println(" Current Balance: ₹" + rs.getBigDecimal("balance"));
        }
    }
    private static void viewTransactions(Connection conn) throws SQLException {
        System.out.print("How many recent transactions do you want to view? ");
        int limit = sc.nextInt();

        PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM transactions " +
                        "WHERE from_account = ? OR to_account = ? " +
                        "ORDER BY created_at DESC LIMIT ?"
        );

        ps.setInt(1, loggedInAccount);
        ps.setInt(2, loggedInAccount);
        ps.setInt(3, limit);
        ResultSet rs = ps.executeQuery();


        System.out.println("\n===== RECENT TRANSACTIONS =====");
        System.out.printf("%-5s %-12s %-12s %-10s %-10s %-20s\n",
                "ID", "From", "To", "Type", "Amount", "Time");
        System.out.println("--------------------------------------------------------------");

        boolean found = false;
        while (rs.next()) {
            found = true;
            System.out.printf("%-5d %-12s %-12s %-10s %-10s %-20s\n",
                    rs.getInt("id"),
                    rs.getString("from_account"),
                    rs.getString("to_account") == null ? "-" : rs.getString("to_account"),
                    rs.getString("type"),
                    rs.getBigDecimal("amount"),
                    rs.getTimestamp("created_at"));
        }

        if (!found) {
            System.out.println("No transactions found.");
        }
    }


    private static void downloadStatement(Connection conn) throws SQLException {
        PreparedStatement accStmt = conn.prepareStatement(
                "SELECT holder_name FROM accounts WHERE account_number=?"
        );
        accStmt.setInt(1, loggedInAccount);
        ResultSet accRs = accStmt.executeQuery();

        if (!accRs.next()) {
            System.out.println("Account not found!");
            return;
        }

        String holderName = accRs.getString("holder_name");

        PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM transactions WHERE from_account=? OR to_account=? ORDER BY created_at DESC"
        );
        ps.setInt(1, loggedInAccount);
        ps.setInt(2, loggedInAccount);
        ResultSet rs = ps.executeQuery();

        String fileName = "Statement_" + loggedInAccount + ".pdf";

        try {
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(fileName));

            document.open();
            document.add(new com.itextpdf.text.Paragraph("BANK ACCOUNT STATEMENT"));
            document.add(new com.itextpdf.text.Paragraph("Account Number: " + loggedInAccount));
            document.add(new com.itextpdf.text.Paragraph("Account Holder: " + holderName));
            document.add(new com.itextpdf.text.Paragraph("Generated On: " + new java.util.Date()));
            document.add(new com.itextpdf.text.Paragraph("\n--------------------------------------------\n"));

            com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(5);
            table.addCell("Type");
            table.addCell("From");
            table.addCell("To");
            table.addCell("Amount");
            table.addCell("Date");

            while (rs.next()) {
                table.addCell(rs.getString("type"));
                table.addCell(rs.getString("from_account"));
                table.addCell(rs.getString("to_account") == null ? "-" : rs.getString("to_account"));
                table.addCell(rs.getBigDecimal("amount").toPlainString());
                table.addCell(rs.getTimestamp("created_at").toString());
            }

            document.add(table);
            document.close();

            System.out.println("\n📄 Statement downloaded successfully: " + fileName);

// Fetch user email to send statement
            PreparedStatement emailStmt = conn.prepareStatement("SELECT email FROM accounts WHERE account_number=?");
            emailStmt.setInt(1, loggedInAccount);
            ResultSet emailRs = emailStmt.executeQuery();

            if (emailRs.next()) {
                String userEmail = emailRs.getString("email");
                sendStatementByEmail(userEmail, fileName);
            } else {
                System.out.println("⚠ Unable to fetch user email for sending statement.");
            }
        }
         catch (Exception e) {
             System.out.println("Error generating PDF: " + e.getMessage());
        }
    }


    private static BigDecimal inputAmountWithRetry(String action) {
        while (true) {
            System.out.print("Enter amount to " + action + ": ");

            // Validate numeric input
            if (!sc.hasNextBigDecimal()) {
                System.out.println(" Invalid input! Enter numbers only.");
                sc.next();
                continue;
            }

            BigDecimal amount = sc.nextBigDecimal();

            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                return amount; //  valid
            }

            System.out.println(" Amount must be greater than 0!");

            while (true) {
                System.out.println("\n1️. Try Again");
                System.out.println("2️. Go to Menu");
                System.out.print("Choose: ");

                if (!sc.hasNextInt()) {
                    System.out.println(" Enter only 1 or 2!");
                    sc.next();
                    continue;
                }

                int choice = sc.nextInt();

                if (choice == 1) {
                    break; // Try again
                } else if (choice == 2) {
                    return BigDecimal.ZERO; // Back to menu
                } else {
                    System.out.println(" Invalid choice! Enter 1 or 2 only.");
                }
            }
        }
    }


}
