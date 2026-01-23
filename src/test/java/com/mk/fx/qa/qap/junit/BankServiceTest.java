package com.mk.fx.qa.qap.junit;

import static org.junit.jupiter.api.Assertions.*;

import com.mk.fx.qa.qap.junit.extension.QAPJunitExtension;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("Banking")
@ExtendWith(QAPJunitExtension.class)
@DisplayName("Bank Service Test Suite")
class BankServiceTest {

  private BankService bankService;

  @BeforeEach
  void setUp() {
    bankService = new BankService();
  }

  // --- Account Creation Tests ---

  @Test
  @DisplayName("Should create account with initial balance")
  @Tag("AccountCreation")
  void shouldCreateAccountWithInitialBalance() {
    bankService.createAccount("ACC001", new BigDecimal("1000.00"));
    assertEquals(new BigDecimal("1000.00"), bankService.getBalance("ACC001"));
  }

  @Test
  @Tag("AccountCreation")
  void shouldCreateAccountWithZeroBalance() {
    bankService.createAccount("ACC002", BigDecimal.ZERO);
    assertEquals(BigDecimal.ZERO, bankService.getBalance("ACC002"));
  }

  @Test
  @DisplayName("Should fail when creating duplicate account")
  @Tag("AccountCreation")
  void shouldFailWhenCreatingDuplicateAccount() {
    bankService.createAccount("ACC003", new BigDecimal("500.00"));
    assertThrows(
        IllegalArgumentException.class,
        () -> bankService.createAccount("ACC003", new BigDecimal("200.00")));
  }

  @Test
  @DisplayName("Should fail when creating account with negative balance")
  @Tag("AccountCreation")
  void shouldFailWhenCreatingAccountWithNegativeBalance() {
    assertThrows(
        IllegalArgumentException.class,
        () -> bankService.createAccount("ACC004", new BigDecimal("-100.00")));
  }

  // --- Deposit Tests ---

  @ParameterizedTest(name = "Deposit {1} into account with balance {0} should result in {2}")
  @CsvSource({
    "0.00, 100.50, 100.50",
    "100.00, 50.25, 150.25",
    "1000.00, 999.99, 1999.99"
  })
  @Tag("Deposit")
  void shouldDepositAmountIntoAccount(
      BigDecimal initialBalance, BigDecimal depositAmount, BigDecimal expectedBalance) {
    bankService.createAccount("ACC005", initialBalance);
    bankService.deposit("ACC005", depositAmount);
    assertEquals(expectedBalance, bankService.getBalance("ACC005"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.00", "-10.00", "-100.50"})
  @Tag("Deposit")
  void shouldFailWhenDepositingInvalidAmount(String invalidAmount) {
    bankService.createAccount("ACC006", new BigDecimal("100.00"));
    assertThrows(
        IllegalArgumentException.class,
        () -> bankService.deposit("ACC006", new BigDecimal(invalidAmount)));
  }

  // --- Withdrawal Tests ---

  @Nested
  @DisplayName("Withdrawal Operations")
  @Tag("Withdrawal")
  class WithdrawalTests {

    @Test
    @DisplayName("Should withdraw valid amount from account")
    void shouldWithdrawValidAmount() {
      bankService.createAccount("ACC007", new BigDecimal("1000.00"));
      bankService.withdraw("ACC007", new BigDecimal("250.00"));
      assertEquals(new BigDecimal("750.00"), bankService.getBalance("ACC007"));
    }

    @Test
    void shouldFailWhenWithdrawingMoreThanBalance() {
      bankService.createAccount("ACC008", new BigDecimal("100.00"));
      assertThrows(
          BankService.InsufficientFundsException.class,
          () -> bankService.withdraw("ACC008", new BigDecimal("200.00")));
    }

    @Test
    @DisplayName("Should fail when withdrawing from non-existent account")
    void shouldFailWhenWithdrawingFromNonExistentAccount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> bankService.withdraw("NONEXISTENT", new BigDecimal("100.00")));
    }

    @ParameterizedTest(name = "Withdraw {1} from {0} should leave {2}")
    @CsvSource({
      "1000.00, 100.00, 900.00",
      "500.00, 250.50, 249.50",
      "100.00, 100.00, 0.00"
    })
    void shouldWithdrawVariousAmounts(
        BigDecimal initialBalance, BigDecimal withdrawalAmount, BigDecimal expectedBalance) {
      bankService.createAccount("ACC009", initialBalance);
      bankService.withdraw("ACC009", withdrawalAmount);
      assertEquals(expectedBalance, bankService.getBalance("ACC009"));
    }

    @Nested
    @DisplayName("Edge Cases")
    class WithdrawalEdgeCases {

      @Test
      void shouldFailWhenWithdrawingZero() {
        bankService.createAccount("ACC011", new BigDecimal("100.00"));
        assertThrows(
            IllegalArgumentException.class,
            () -> bankService.withdraw("ACC011", BigDecimal.ZERO));
      }

      @Test
      @Disabled("Known issue: negative withdrawal amounts not properly validated")
      void shouldFailWhenWithdrawingNegativeAmount() {
        bankService.createAccount("ACC012", new BigDecimal("100.00"));
        assertThrows(
            IllegalArgumentException.class,
            () -> bankService.withdraw("ACC012", new BigDecimal("-50.00")));
      }
    }
  }

  // --- Transfer Tests ---

  @Nested
  @DisplayName("Transfer Operations")
  @Tag("Transfer")
  class TransferTests {

    @BeforeEach
    void setUpAccounts() {
      bankService.createAccount("FROM001", new BigDecimal("1000.00"));
      bankService.createAccount("TO001", new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Should transfer amount between accounts")
    void shouldTransferAmountBetweenAccounts() {
      bankService.transfer("FROM001", "TO001", new BigDecimal("200.00"));
      assertEquals(new BigDecimal("800.00"), bankService.getBalance("FROM001"));
      assertEquals(new BigDecimal("700.00"), bankService.getBalance("TO001"));
    }

    @Test
    void shouldFailWhenTransferringInsufficientFunds() {
      assertThrows(
          BankService.InsufficientFundsException.class,
          () -> bankService.transfer("FROM001", "TO001", new BigDecimal("2000.00")));
    }

    @ParameterizedTest(name = "Transfer {2} from account with {0} to account with {1}")
    @CsvSource({
      "1000.00, 500.00, 100.00",
      "2000.00, 0.00, 500.00",
      "100.00, 100.00, 50.00"
    })
    void shouldTransferVariousAmounts(
        BigDecimal fromBalance, BigDecimal toBalance, BigDecimal transferAmount) {
      bankService.createAccount("FROM002", fromBalance);
      bankService.createAccount("TO002", toBalance);
      bankService.transfer("FROM002", "TO002", transferAmount);
      assertEquals(
          fromBalance.subtract(transferAmount), bankService.getBalance("FROM002"));
      assertEquals(toBalance.add(transferAmount), bankService.getBalance("TO002"));
    }
  }

  // --- Account Existence Tests ---

  @Test
  @DisplayName("Should return true for existing account")
  @Tag("AccountQuery")
  void shouldReturnTrueForExistingAccount() {
    bankService.createAccount("ACC013", new BigDecimal("100.00"));
    assertTrue(bankService.accountExists("ACC013"));
  }

  @Test
  @Tag("AccountQuery")
  void shouldReturnFalseForNonExistentAccount() {
    assertFalse(bankService.accountExists("NONEXISTENT"));
  }


  // --- Disabled Tests ---

  @Test
  @Disabled("Feature not yet implemented")
  @DisplayName("Should calculate interest on account balance")
  @Tag("Interest")
  void shouldCalculateInterestOnAccountBalance() {
    bankService.createAccount("ACC016", new BigDecimal("1000.00"));
    // Interest calculation not implemented yet
    fail("Interest calculation feature not implemented");
  }

  @Test
  @Disabled("Waiting for external API integration")
  @Tag("External")
  void shouldSyncWithExternalBankingSystem() {
    fail("External API integration pending");
  }

  // --- After Each Cleanup ---

  @AfterEach
  void tearDown() {
    // Cleanup if needed
  }
}
