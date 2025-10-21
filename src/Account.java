import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Account {
    private int accountNumber;
    private String holderName;
    private String email;
    private String pin;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    public Account(String holderName, String email, String pin, BigDecimal balance) {
        this.holderName = holderName;
        this.email = email;
        this.pin = pin;
        this.balance = balance;
        this.createdAt = LocalDateTime.now();
    }

    public Account(int accountNumber, String holderName, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.balance = balance;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void deposit(BigDecimal amount) throws Exception {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Amount must be positive");
        }
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) throws Exception {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new Exception("Insufficient funds");
        }
        balance = balance.subtract(amount);
    }
}
