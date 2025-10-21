import java.math.BigDecimal;
import java.sql.*;
import java.util.Scanner;

public class AccountApp {
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection conn = MyJDBC.getConnection()) {
            while (true) {
                System.out.println("\n=== BANKING SYSTEM MENU ===");
                System.out.println("1. Create Account");
                System.out.println("2. Deposit");
                System.out.println("3. Withdraw");
                System.out.println("4. Transfer Money");
                System.out.println("5. View Balance");
                System.out.println("6. Exit");
                System.out.print("Enter choice: ");
                int choice = sc.nextInt();

                switch (choice) {
                    case 1 -> createAccount(conn);
                    case 2 -> deposit(conn);
                    case 3 -> withdraw(conn);
                    case 4 -> transfer(conn);
                    case 5 -> viewBalance(conn);
                    case 6 -> {
                        System.out.println(" Thank you for banking with us!");
                        return;
                    }
                    default -> System.out.println(" Invalid choice! Try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void createAccount(Connection conn) {
        try {
            sc.nextLine();
            System.out.print("Enter Holder Name: ");
            String name = sc.nextLine();
            System.out.print("Enter Email: ");
            String email = sc.nextLine();


            PreparedStatement check = conn.prepareStatement("SELECT * FROM accounts WHERE email = ?");
            check.setString(1, email);
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                System.out.println(" Account with this email already exists!");
                return;
            }

            System.out.print("Set a 4-digit PIN: ");
            String pin = sc.nextLine();
            System.out.print("Enter Initial Deposit: ");
            BigDecimal deposit = sc.nextBigDecimal();

            String sql = "INSERT INTO accounts (holder_name, email, pin, balance) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, pin);
            ps.setBigDecimal(4, deposit);
            ps.executeUpdate();

            ResultSet    rs2 = ps.getGeneratedKeys();
            if (rs2.next()) {
                int accNo = rs2.getInt(1);
                System.out.println(" Account created successfully! Your Account Number: " + accNo);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println(" Account with this email already exists!");
        } catch (SQLException e) {
            System.out.println(" Error creating account: " + e.getMessage());
        }
    }


    private static boolean verifyPin(Connection conn, int accNo) throws SQLException {
        System.out.print("Enter your 4-digit PIN: ");
        String inputPin = sc.next();

        String sql = "SELECT pin FROM accounts WHERE account_number = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, accNo);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            String correctPin = rs.getString("pin");
            if (correctPin.equals(inputPin)) {
                return true;
            } else {
                System.out.println(" Incorrect PIN!");
                return false;
            }
        } else {
            System.out.println(" Account not found!");
            return false;
        }
    }


    private static void deposit(Connection conn) throws SQLException {
        System.out.print("Enter Account Number: ");
        int accNo = sc.nextInt();

        if (!verifyPin(conn, accNo)) return;

        System.out.print("Enter Amount to Deposit: ");
        BigDecimal amount = sc.nextBigDecimal();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println(" Invalid amount!");
            return;
        }

        String update = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
        PreparedStatement ps = conn.prepareStatement(update);
        ps.setBigDecimal(1, amount);
        ps.setInt(2, accNo);
        int rows = ps.executeUpdate();

        if (rows > 0) {
            logTransaction(conn, accNo, accNo, amount, "Deposit");
            showUpdatedBalance(conn, accNo);
            System.out.println(" Deposit successful!");
        } else {
            System.out.println(" Account not found!");
        }
    }


    private static void withdraw(Connection conn) throws SQLException {
        System.out.print("Enter Account Number: ");
        int accNo = sc.nextInt();

        if (!verifyPin(conn, accNo)) return;

        System.out.print("Enter Amount to Withdraw: ");
        BigDecimal amount = sc.nextBigDecimal();

        String check = "SELECT balance FROM accounts WHERE account_number = ?";
        PreparedStatement ps = conn.prepareStatement(check);
        ps.setInt(1, accNo);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            BigDecimal balance = rs.getBigDecimal("balance");
            if (balance.compareTo(amount) >= 0 && amount.compareTo(BigDecimal.ZERO) > 0) {
                String update = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
                PreparedStatement ps2 = conn.prepareStatement(update);
                ps2.setBigDecimal(1, amount);
                ps2.setInt(2, accNo);
                ps2.executeUpdate();

                logTransaction(conn, accNo, accNo, amount, "Withdraw");
                showUpdatedBalance(conn, accNo);
                System.out.println(" Withdrawal successful!");
            } else {
                System.out.println(" Insufficient balance or invalid amount!");
            }
        } else {
            System.out.println(" Account not found!");
        }
    }


    private static void transfer(Connection conn) throws SQLException {
        System.out.print("Enter Sender Account Number: ");
        int fromAcc = sc.nextInt();

        if (!verifyPin(conn, fromAcc)) return;

        System.out.print("Enter Receiver Account Number: ");
        int toAcc = sc.nextInt();
        System.out.print("Enter Amount to Transfer: ");
        BigDecimal amount = sc.nextBigDecimal();

        conn.setAutoCommit(false);
        try {
            PreparedStatement ps1 = conn.prepareStatement("SELECT balance FROM accounts WHERE account_number = ?");
            ps1.setInt(1, fromAcc);
            ResultSet rs1 = ps1.executeQuery();

            if (rs1.next()) {
                BigDecimal balance = rs1.getBigDecimal("balance");
                if (balance.compareTo(amount) >= 0 && amount.compareTo(BigDecimal.ZERO) > 0) {
                    PreparedStatement debit = conn.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE account_number = ?");
                    debit.setBigDecimal(1, amount);
                    debit.setInt(2, fromAcc);
                    debit.executeUpdate();

                    PreparedStatement credit = conn.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE account_number = ?");
                    credit.setBigDecimal(1, amount);
                    credit.setInt(2, toAcc);
                    credit.executeUpdate();

                    logTransaction(conn, fromAcc, toAcc, amount, "Transfer");
                    conn.commit();

                    showUpdatedBalance(conn, fromAcc);
                    System.out.println(" Transfer successful!");
                } else {
                    System.out.println(" Insufficient balance or invalid amount!");
                }
            } else {
                System.out.println(" Sender account not found!");
            }
        } catch (SQLException e) {
            conn.rollback();
            System.out.println(" Transfer failed: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }


    private static void viewBalance(Connection conn) throws SQLException {
        System.out.print("Enter Account Number: ");
        int accNo = sc.nextInt();

        if (!verifyPin(conn, accNo)) return;

        showUpdatedBalance(conn, accNo);
    }


    private static void showUpdatedBalance(Connection conn, int accNo) throws SQLException {
        String sql = "SELECT holder_name, balance FROM accounts WHERE account_number = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, accNo);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            System.out.println("--------------------------");
            System.out.println(" Account Holder: " + rs.getString("holder_name"));
            System.out.println(" Current Balance: ₹" + rs.getBigDecimal("balance"));
            System.out.println("--------------------------");
        }
    }


    private static void logTransaction(Connection conn, int fromAcc, int toAcc, BigDecimal amount, String type) throws SQLException {
        String sql = "INSERT INTO transactions (from_account, to_account, amount, type) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, fromAcc);
        ps.setInt(2, toAcc);
        ps.setBigDecimal(3, amount);
        ps.setString(4, type);
        ps.executeUpdate();
    }
}
