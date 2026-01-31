package com.example.testapp;

import static org.junit.jupiter.api.Assertions.*;

import com.mk.fx.qa.qap.junit.extension.QAPJunitExtension;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(QAPJunitExtension.class)
@DisplayName("Payment Processor Tests")
@Tag("payment")
@Tag("financial")
class PaymentProcessorTest {

  private static final Logger logger = LoggerFactory.getLogger(PaymentProcessorTest.class);
  private PaymentProcessor paymentProcessor;

  @BeforeAll
  static void setupClass() {
    logger.info("=== Starting Payment Processor Test Suite ===");
    logger.info("Initializing payment gateway connection");
  }

  @AfterAll
  static void teardownClass() {
    logger.info("=== Payment Processor Test Suite Complete ===");
    logger.info("Closing payment gateway connection");
  }

  @BeforeEach
  void setup() {
    logger.info("Creating new payment processor instance");
    paymentProcessor = new PaymentProcessor();
  }

  @Test
  @DisplayName("Should process valid credit card payment")
  void testProcessCreditCardPayment() {
    logger.info("Testing credit card payment processing");

    String cardNumber = "4532-1234-5678-9012";
    BigDecimal amount = new BigDecimal("99.99");

    logger.debug("Processing payment - Card: {}, Amount: ${}", maskCard(cardNumber), amount);

    PaymentResult result = paymentProcessor.processPayment(cardNumber, amount);

    assertTrue(result.isSuccess());
    assertEquals("APPROVED", result.getStatus());
    assertNotNull(result.getTransactionId());
    logger.info(
        "Payment approved - Transaction ID: {}, Amount: ${}", result.getTransactionId(), amount);
  }

  @ParameterizedTest(name = "Processing ${0} payment")
  @ValueSource(strings = {"10.00", "25.50", "100.00", "999.99", "1500.00"})
  @DisplayName("Should process various payment amounts")
  void testVariousPaymentAmounts(String amountStr) {
    logger.info("Testing payment amount: ${}", amountStr);

    BigDecimal amount = new BigDecimal(amountStr);
    String cardNumber = "4532-1234-5678-9012";

    logger.debug("Processing payment of ${}", amount);
    PaymentResult result = paymentProcessor.processPayment(cardNumber, amount);

    assertTrue(result.isSuccess());
    logger.info("Payment of ${} processed successfully", amount);
  }

  @ParameterizedTest(name = "{0} - Fee: ${1}, Total: ${2}")
  @CsvSource({
    "Standard Processing, 10.00, 0.30, 10.30",
    "Express Processing, 50.00, 2.50, 52.50",
    "International, 100.00, 5.00, 105.00",
    "Premium Service, 250.00, 10.00, 260.00"
  })
  @DisplayName("Should calculate processing fees correctly")
  void testProcessingFees(String type, String baseAmount, String fee, String expectedTotal) {
    logger.info("Testing {} - Base: ${}, Fee: ${}", type, baseAmount, fee);

    BigDecimal base = new BigDecimal(baseAmount);
    BigDecimal feeAmount = new BigDecimal(fee);
    BigDecimal expected = new BigDecimal(expectedTotal);

    logger.debug("Calculating total: {} + {} = {}", base, feeAmount, expected);

    BigDecimal total = paymentProcessor.calculateTotal(base, feeAmount);

    assertEquals(expected, total);
    logger.info("Fee calculation correct for {}: ${}", type, total);
  }

  @Test
  @DisplayName("Should reject payment below minimum amount")
  void testMinimumPaymentAmount() {
    logger.info("Testing minimum payment validation");

    BigDecimal tooSmall = new BigDecimal("0.50");
    String cardNumber = "4532-1234-5678-9012";

    logger.warn("Attempting payment below minimum: ${}", tooSmall);

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> paymentProcessor.processPayment(cardNumber, tooSmall));

    logger.error("Payment rejected: {}", exception.getMessage());
    //assertTrue(exception.getMessage().contains("should fail"));
  }

  @Test
  @DisplayName("Should reject invalid card number")
  void testInvalidCardNumber() {
    logger.info("Testing invalid card number handling");

    String invalidCard = "1234-5678";
    BigDecimal amount = new BigDecimal("50.00");

    logger.warn("Testing with invalid card: {}", maskCard(invalidCard));

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> paymentProcessor.processPayment(invalidCard, amount));

    logger.error("Card validation failed: {}", exception.getMessage());
    assertTrue(exception.getMessage().contains("Invalid card"));
  }

  @Test
  @DisplayName("Should handle insufficient funds")
  void testInsufficientFunds() {
    logger.info("Testing insufficient funds scenario");

    String cardNumber = "4532-0000-0000-0001"; // Test card for insufficient funds
    BigDecimal amount = new BigDecimal("10000.00");

    logger.warn("Processing large amount ${} that will be declined", amount);

    PaymentResult result = paymentProcessor.processPayment(cardNumber, amount);

    assertFalse(result.isSuccess());
    assertEquals("DECLINED", result.getStatus());
    logger.warn("Payment declined as expected: {}", result.getStatus());
  }

  @Nested
  @DisplayName("Refund Operations")
  class RefundTests {

    private String transactionId;

    @BeforeEach
    void setupRefundTest() {
      logger.info("Setting up refund test - creating initial payment");

      PaymentResult payment =
          paymentProcessor.processPayment("4532-1234-5678-9012", new BigDecimal("100.00"));
      transactionId = payment.getTransactionId();

      logger.debug("Initial payment created - Transaction ID: {}", transactionId);
    }

    @Test
    @DisplayName("Should process full refund")
    void testFullRefund() {
      logger.info("Testing full refund for transaction: {}", transactionId);

      BigDecimal refundAmount = new BigDecimal("100.00");
      logger.debug("Processing refund of ${}", refundAmount);

      RefundResult result = paymentProcessor.processRefund(transactionId, refundAmount);

      assertTrue(result.isSuccess());
      assertEquals("REFUNDED", result.getStatus());
      logger.info("Full refund processed successfully: {}", transactionId);
    }

    @Test
    @DisplayName("Should process partial refund")
    void testPartialRefund() {
      logger.info("Testing partial refund for transaction: {}", transactionId);

      BigDecimal refundAmount = new BigDecimal("50.00");
      logger.debug("Processing partial refund of ${}", refundAmount);

      RefundResult result = paymentProcessor.processRefund(transactionId, refundAmount);

      assertTrue(result.isSuccess());
      logger.info("Partial refund of ${} processed successfully", refundAmount);
    }
  }

  private String maskCard(String cardNumber) {
    if (cardNumber.length() > 4) {
      return "****" + cardNumber.substring(cardNumber.length() - 4);
    }
    return "****";
  }

  // Payment domain classes
  static class PaymentResult {
    private final boolean success;
    private final String status;
    private final String transactionId;

    public PaymentResult(boolean success, String status, String transactionId) {
      this.success = success;
      this.status = status;
      this.transactionId = transactionId;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getStatus() {
      return status;
    }

    public String getTransactionId() {
      return transactionId;
    }
  }

  static class RefundResult {
    private final boolean success;
    private final String status;

    public RefundResult(boolean success, String status) {
      this.success = success;
      this.status = status;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getStatus() {
      return status;
    }
  }

  static class PaymentProcessor {
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1.00");
    private int transactionCounter = 1000;

    public PaymentResult processPayment(String cardNumber, BigDecimal amount) {
      if (amount.compareTo(MIN_AMOUNT) < 0) {
        throw new IllegalArgumentException("Amount below minimum of $" + MIN_AMOUNT);
      }

      if (!isValidCard(cardNumber)) {
        throw new IllegalArgumentException("Invalid card number: " + cardNumber);
      }

      // Simulate insufficient funds for specific test card
      if (cardNumber.equals("4532-0000-0000-0001")) {
        return new PaymentResult(false, "DECLINED", null);
      }

      String txnId = "TXN-" + (transactionCounter++);
      return new PaymentResult(true, "APPROVED", txnId);
    }

    public BigDecimal calculateTotal(BigDecimal base, BigDecimal fee) {
      return base.add(fee);
    }

    public RefundResult processRefund(String transactionId, BigDecimal amount) {
      // Simulate refund processing
      return new RefundResult(true, "REFUNDED");
    }

    private boolean isValidCard(String cardNumber) {
      return cardNumber != null && cardNumber.replace("-", "").length() >= 13;
    }
  }
}
