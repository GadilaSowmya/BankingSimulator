# 🏦 Banking System in Java (JDBC + MySQL)

This is a **console-based Banking System** implemented in **Java** using **JDBC** and **MySQL**.  
It allows users to create accounts, securely log in, deposit & withdraw funds, transfer money, and view transaction history with proper database handling.

---

## 📌 Features

| Feature | Description |
|--------|-------------|
✅ Create Account | User registration with validation  
✅ Login System | Secure login using PIN  
✅ Deposit Money | Add funds to account  
✅ Withdraw Money | Balance check + withdrawal  
✅ Transfer Money | Transfer to other accounts (with rollback safety)  
✅ View Balance | Display current balance  
✅ Transactions History | Shows recent transactions  
✅ Regex Validation | Email, Name, PIN verification  
✅ DB Persistence | MySQL storage + JDBC integration  

---

## 🎯 Objectives

- Build a reliable banking simulation
- Implement JDBC CRUD operations
- Ensure secure and validated transactions
- Practice exception handling & SQL commit/rollback
- Use BigDecimal for financial accuracy

---

## 🧠 Tech Stack

| Technology | Purpose |
|-----------|--------|
Java | Core application logic  
JDBC | Database connectivity  
MySQL | Data storage  
BigDecimal | Accurate money calculations  
Regex | Input validation  

---

## 📂 System Workflow

1. User opens the app
2. Creates account / logs in
3. Performs banking operations
4. Data stored & retrieved from MySQL
5. All transactions logged with timestamp  

---
## How BigDecimal is Used for Money
In Java, floating-point types like `float` and `double` can cause rounding errors when dealing with currency.  
To ensure **accurate financial calculations**, this project uses **BigDecimal** for representing the account balance and transaction amounts.
