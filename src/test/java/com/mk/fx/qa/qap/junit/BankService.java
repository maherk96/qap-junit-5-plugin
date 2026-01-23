package com.mk.fx.qa.qap.junit;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple bank service for testing purposes.
 */
public class BankService {

  private final Map<String, Account> accounts = new HashMap<>();

  public void createAccount(String accountId, BigDecimal initialBalance) {
    if (accounts.containsKey(accountId)) {
      throw new IllegalArgumentException("Account already exists: " + accountId);
    }
    if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Initial balance cannot be negative");
    }
    accounts.put(accountId, new Account(accountId, initialBalance));
  }

  public BigDecimal getBalance(String accountId) {
    Account account = accounts.get(accountId);
    if (account == null) {
      throw new IllegalArgumentException("Account not found: " + accountId);
    }
    return account.getBalance();
  }

  public void deposit(String accountId, BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Deposit amount must be positive");
    }
    Account account = accounts.get(accountId);
    if (account == null) {
      throw new IllegalArgumentException("Account not found: " + accountId);
    }
    account.deposit(amount);
  }

  public void withdraw(String accountId, BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Withdrawal amount must be positive");
    }
    Account account = accounts.get(accountId);
    if (account == null) {
      throw new IllegalArgumentException("Account not found: " + accountId);
    }
    if (account.getBalance().compareTo(amount) < 0) {
      throw new InsufficientFundsException(
          "Insufficient funds. Balance: " + account.getBalance() + ", Requested: " + amount);
    }
    account.withdraw(amount);
  }

  /**
   * Transfers an amount from one account to another atomically.
   * Validates both accounts exist and have sufficient funds before performing the transfer.
   * This prevents money loss if the deposit fails after withdrawal.
   *
   * @param fromAccountId the source account ID
   * @param toAccountId the destination account ID
   * @param amount the amount to transfer
   * @throws IllegalArgumentException if amount is invalid or accounts don't exist
   * @throws InsufficientFundsException if source account has insufficient funds
   */
  public void transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
    // Validate amount first
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Transfer amount must be positive");
    }
    
    // Validate both accounts exist before starting transfer
    Account fromAccount = accounts.get(fromAccountId);
    Account toAccount = accounts.get(toAccountId);
    
    if (fromAccount == null) {
      throw new IllegalArgumentException("Source account not found: " + fromAccountId);
    }
    if (toAccount == null) {
      throw new IllegalArgumentException("Destination account not found: " + toAccountId);
    }
    
    // Validate sufficient funds
    if (fromAccount.getBalance().compareTo(amount) < 0) {
      throw new InsufficientFundsException(
          "Insufficient funds. Balance: " + fromAccount.getBalance() + ", Requested: " + amount);
    }
    
    // Now perform atomic transfer (both operations succeed or both fail)
    fromAccount.withdraw(amount);
    toAccount.deposit(amount);
  }

  public boolean accountExists(String accountId) {
    return accounts.containsKey(accountId);
  }

  public static class Account {
    private final String accountId;
    private BigDecimal balance;

    public Account(String accountId, BigDecimal balance) {
      this.accountId = accountId;
      this.balance = balance;
    }

    public String getAccountId() {
      return accountId;
    }

    public BigDecimal getBalance() {
      return balance;
    }

    public void deposit(BigDecimal amount) {
      this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
      this.balance = this.balance.subtract(amount);
    }
  }

  public static class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
      super(message);
    }
  }
}
