import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Account {
    private String accountNumber;
    private String holderName;
    private String email;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    public Account(String accountNumber, String holderName, String email, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.email = email;
        this.balance = balance;
        this.createdAt = LocalDateTime.now();
    }

    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Deposit amount must be positive!");
            return;
        }
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) throws Exception {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Withdrawal amount must be positive!");
        }
        if (amount.compareTo(balance) > 0) {
            throw new Exception("Insufficient funds!");
        }
        balance = balance.subtract(amount);
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
