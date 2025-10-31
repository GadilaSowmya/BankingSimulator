
import java.math.BigDecimal;
import java.sql.*;
import java.util.Scanner;

public class AccountApp {
    private static final Scanner sc = new Scanner(System.in);
    private static Integer loggedInAccount = null; // Store logged-in user's account number


    public static void main(String[] args) {
        try (Connection conn = MyJDBC.getConnection()) {
            while (true) {
                System.out.println("\n===== BANKING SYSTEM =====");
                System.out.println("1 Create Account");
                System.out.println("2 Login");
                System.out.println("3️ Exit");
                System.out.print("Choose an option: ");
                int choice = sc.nextInt();

                switch (choice) {
                    case 1 -> createAccount(conn);
                    case 2 -> login(conn);
                    case 3 -> {
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
        System.out.print("Enter Name: ");
        sc.nextLine();
        String name = sc.nextLine().trim();

        // Validating name
        if (!name.matches("^[A-Za-z]+( [A-Za-z]+)*$")) {
            System.out.println(" Invalid name! Only letters are allowed.");
            return;
        }

        System.out.print("Enter Email: ");
        String email = sc.next();

        //  Validate Email Format
        if (!email.matches("^[a-z0-9+_.-]+@[a-z0-9.-]+$")) {
            System.out.println(" Invalid email format! Please enter a valid email like example@gmail.com");
            return;
        }

        //  Check if email already exists
        PreparedStatement checkEmail = conn.prepareStatement("SELECT * FROM accounts WHERE email = ?");
        checkEmail.setString(1, email);
        ResultSet rs = checkEmail.executeQuery();
        if (rs.next()) {
            System.out.println(" Account with this email already exists!");
            return;
        }

        System.out.print("Set 4-digit PIN: ");
        String pin = sc.next();

        // Validate PIN Format
        if (!pin.matches("\\d{4}")) {
            System.out.println(" PIN must be exactly 4 digits!");
            return;
        }

        System.out.print("Enter Initial Deposit: ");
        BigDecimal balance = sc.nextBigDecimal();

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println(" Initial deposit must be positive!");
            return;
        }

        if (balance.compareTo(new BigDecimal("500")) < 0) {
            System.out.println(" Minimum initial deposit is 500");
            return;
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
            System.out.println(" Account created successfully! Your Account Number: " + keys.getInt(1));
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
                System.out.println("\n Login successful! Welcome, " + rs.getString("holder_name") + "!");
                accountMenu(conn);
                return; // exit login loop after success
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

    private static void accountMenu(Connection conn) throws SQLException {
        while (true) {
            System.out.println("\n===== ACCOUNT MENU =====");
            System.out.println("1️ Deposit");
            System.out.println("2️ Withdraw");
            System.out.println("3️ Transfer");
            System.out.println("4️ View Balance");
            System.out.println("5 View Transactions");
            System.out.println("6️ Logout");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();

            switch (choice) {
                case 1 -> deposit(conn);
                case 2 -> withdraw(conn);
                case 3 -> transfer(conn);
                case 4 -> viewBalance(conn, loggedInAccount);
                case 5 -> viewTransactions(conn);
                case 6 -> {
                    System.out.println(" Logged out successfully!");
                    loggedInAccount = null;
                    return;
                }
                default -> System.out.println(" Invalid choice.");
            }
        }
    }

    private static void deposit(Connection conn) throws SQLException {
        BigDecimal amount = inputAmountWithRetry("deposit");
        if (amount.compareTo(BigDecimal.ZERO) == 0) return; // exit to menu

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println(" Invalid amount.");
            return;
        }

        PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE account_number=?");
        ps.setBigDecimal(1, amount);
        ps.setInt(2, loggedInAccount);
        ps.executeUpdate();

        System.out.println(" Deposit successful!");
        viewBalance(conn, loggedInAccount);
    }

    private static void withdraw(Connection conn) throws SQLException {
        BigDecimal amount = inputAmountWithRetry("withdraw");
        if (amount.compareTo(BigDecimal.ZERO) == 0) return; // go back to menu

        PreparedStatement ps = conn.prepareStatement("SELECT balance FROM accounts WHERE account_number=?");
        ps.setInt(1, loggedInAccount);
        ResultSet rs = ps.executeQuery();


        if (rs.next()) {
            BigDecimal balance = rs.getBigDecimal("balance");
            if (balance.compareTo(amount) >= 0) {
                PreparedStatement update = conn.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE account_number=?");
                update.setBigDecimal(1, amount);
                update.setInt(2, loggedInAccount);
                update.executeUpdate();

                System.out.println(" Withdrawal successful!");
                viewBalance(conn, loggedInAccount);
            } else {
                System.out.println(" Insufficient funds!");
            }
        }
        PreparedStatement logDep = conn.prepareStatement(
                "INSERT INTO transactions(from_account, to_account, amount, type) VALUES (?, ?, ?, ?)"
        );
        logDep.setInt(1, loggedInAccount);
        logDep.setNull(2, Types.INTEGER);
        logDep.setBigDecimal(3, amount);
        logDep.setString(4, "DEPOSIT");
        logDep.executeUpdate();

    }


    private static void transfer(Connection conn) throws SQLException {
        System.out.print("Enter Receiver Account Number: ");
        int toAcc = sc.nextInt();

        if (toAcc == loggedInAccount) {
            System.out.println(" Cannot transfer to your own account!");
            return;
        }

        // Validate receiver account & fetch name
        PreparedStatement check = conn.prepareStatement("SELECT holder_name FROM accounts WHERE account_number=?");
        check.setInt(1, toAcc);
        ResultSet rs = check.executeQuery();

        if (!rs.next()) {
            System.out.println(" Receiver account not found! Try again.");
            return;
        }

        String receiverName = rs.getString("holder_name");
        System.out.println(" Receiver Found: " + receiverName);

        // Ask confirmation
        System.out.print("Confirm transfer to " + receiverName + "? (Y/N): ");
        String confirm = sc.next();
        if (!confirm.equalsIgnoreCase("Y")) {
            System.out.println(" Transfer cancelled.");
            return;
        }

        // Get amount with retry logic
        BigDecimal amount = inputAmountWithRetry("transfer");
        if (amount.compareTo(BigDecimal.ZERO) == 0) return;

        conn.setAutoCommit(false);
        try {
            PreparedStatement withdraw = conn.prepareStatement(
                    "UPDATE accounts SET balance = balance - ? WHERE account_number=? AND balance >= ?"
            );
            withdraw.setBigDecimal(1, amount);
            withdraw.setInt(2, loggedInAccount);
            withdraw.setBigDecimal(3, amount);
            int rows = withdraw.executeUpdate();

            if (rows == 0) {
                System.out.println(" Insufficient funds!");
                conn.rollback();
                return;
            }

            // Deposit into receiver
            PreparedStatement deposit = conn.prepareStatement(
                    "UPDATE accounts SET balance = balance + ? WHERE account_number=?"
            );
            deposit.setBigDecimal(1, amount);
            deposit.setInt(2, toAcc);
            deposit.executeUpdate();


// Log credit transaction for receiver
            PreparedStatement logCredit = conn.prepareStatement(
                    "INSERT INTO transactions(from_account, to_account, amount, type) VALUES (?, ?, ?, ?)"
            );
            logCredit.setInt(1, loggedInAccount);
            logCredit.setInt(2, toAcc);
            logCredit.setBigDecimal(3, amount);
            logCredit.setString(4, "Transfer");
            logCredit.executeUpdate();


            conn.commit();
            System.out.println(" Transfer successful to " + receiverName + "!");
            viewBalance(conn, loggedInAccount);

        } catch (SQLException e) {
            conn.rollback();
            System.out.println(" Transfer failed: " + e.getMessage());
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
