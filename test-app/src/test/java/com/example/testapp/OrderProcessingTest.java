package com.example.testapp;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisplayName("Order Processing Tests")
@Tag("order-processing")
@Tag("e2e")
class OrderProcessingTest {

  private static final Logger logger = LoggerFactory.getLogger(OrderProcessingTest.class);
  private OrderService orderService;

  @BeforeAll
  static void setupClass() {
    logger.info("╔══════════════════════════════════════════════════════════╗");
    logger.info("║   Starting Order Processing End-to-End Test Suite      ║");
    logger.info("╚══════════════════════════════════════════════════════════╝");
    logger.debug("Initializing order processing system");
  }

  @BeforeEach
  void setup() {
    logger.info("→ Setting up order service for test");
    orderService = new OrderService();
    logger.debug("Order service initialized with empty order queue");
  }

  @Test
  @DisplayName("✓ Should create and process simple order")
  void testCreateSimpleOrder() {
    logger.info("▶ Testing simple order creation and processing");

    String customerId = "CUST-12345";
    logger.debug("Creating order for customer: {}", customerId);

    Order order = orderService.createOrder(customerId);
    logger.info("Order created: {}", order.getOrderId());

    orderService.addItem(order.getOrderId(), "ITEM-001", 2, 29.99);
    orderService.addItem(order.getOrderId(), "ITEM-002", 1, 49.99);
    logger.debug("Added 2 items to order {}", order.getOrderId());

    double total = orderService.calculateTotal(order.getOrderId());
    logger.info("Order total calculated: ${}", String.format("%.2f", total));

    assertEquals(109.97, total, 0.01);
    logger.info("✓ Simple order test completed successfully");
  }

  @Test
  @DisplayName("✓ Should apply discount code")
  void testApplyDiscountCode() {
    logger.info("▶ Testing discount code application");

    Order order = orderService.createOrder("CUST-001");
    orderService.addItem(order.getOrderId(), "ITEM-001", 1, 100.00);

    String discountCode = "SAVE20";
    logger.debug("Applying discount code: {}", discountCode);

    boolean applied = orderService.applyDiscount(order.getOrderId(), discountCode);

    assertTrue(applied);
    double total = orderService.calculateTotal(order.getOrderId());
    assertEquals(80.00, total, 0.01);
    logger.info("Discount applied successfully. Final total: ${}", String.format("%.2f", total));
  }

  @ParameterizedTest(name = "Order status: {0} → {1}")
  @CsvSource({
    "PENDING, CONFIRMED, Confirming order",
    "CONFIRMED, SHIPPED, Shipping order",
    "SHIPPED, DELIVERED, Delivering order"
  })
  @DisplayName("✓ Should transition order through valid statuses")
  void testOrderStatusTransitions(String fromStatus, String toStatus, String action) {
    logger.info("▶ Testing status transition: {} → {}", fromStatus, toStatus);

    Order order = orderService.createOrder("CUST-STATUS");
    orderService.addItem(order.getOrderId(), "ITEM-001", 1, 50.00);

    // Set initial status
    orderService.updateStatus(order.getOrderId(), fromStatus);
    logger.debug("Order {} status set to {}", order.getOrderId(), fromStatus);

    // Transition to next status
    logger.info("→ {} ({})", action, toStatus);
    orderService.updateStatus(order.getOrderId(), toStatus);

    assertEquals(toStatus, orderService.getOrderStatus(order.getOrderId()));
    logger.info("✓ Status transition successful: {} → {}", fromStatus, toStatus);
  }

  @Test
  @DisplayName("✓ Should calculate shipping cost based on weight")
  void testShippingCalculation() {
    logger.info("▶ Testing shipping cost calculation");

    Order order = orderService.createOrder("CUST-SHIPPING");

    // Add items with different weights
    orderService.addItemWithWeight(order.getOrderId(), "HEAVY-001", 1, 50.00, 5.0);
    orderService.addItemWithWeight(order.getOrderId(), "LIGHT-002", 2, 25.00, 0.5);

    logger.debug("Order {} has total weight of 6.0 kg", order.getOrderId());

    double shippingCost = orderService.calculateShipping(order.getOrderId());

    assertTrue(shippingCost > 0);
    logger.info("Shipping cost calculated: ${}", String.format("%.2f", shippingCost));
  }

  @Test
  @DisplayName("✗ Should fail for invalid discount code")
  void testInvalidDiscountCode() {
    logger.info("▶ Testing invalid discount code handling");

    Order order = orderService.createOrder("CUST-INVALID");
    orderService.addItem(order.getOrderId(), "ITEM-001", 1, 100.00);

    String invalidCode = "EXPIRED123";
    logger.warn("⚠ Attempting to apply invalid discount code: {}", invalidCode);

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> orderService.applyDiscount(order.getOrderId(), invalidCode));

    logger.error("✗ Expected failure: {}", exception.getMessage());
    assertTrue(exception.getMessage().contains("Invalid"));
    logger.info("✓ Invalid discount properly rejected");
  }

  @Test
  @DisplayName("✗ Should fail when adding item to cancelled order")
  void testAddItemToCancelledOrder() {
    logger.info("▶ Testing add item to cancelled order");

    Order order = orderService.createOrder("CUST-CANCEL");
    logger.debug("Created order {}", order.getOrderId());

    orderService.cancelOrder(order.getOrderId());
    logger.warn("⚠ Order {} has been cancelled", order.getOrderId());

    logger.warn("⚠ Attempting to add item to cancelled order");
    Exception exception =
        assertThrows(
            IllegalStateException.class,
            () -> orderService.addItem(order.getOrderId(), "ITEM-001", 1, 50.00));

    logger.error("✗ Expected failure: {}", exception.getMessage());
    assertTrue(exception.getMessage().contains("cancelled"));
  }

  @Test
  @DisplayName("✓ Should process order with multiple payment methods")
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void testMultiplePaymentMethods() {
    logger.info("▶ Testing order with split payment");

    Order order = orderService.createOrder("CUST-SPLIT");
    orderService.addItem(order.getOrderId(), "ITEM-001", 1, 100.00);

    logger.debug("Processing split payment: Credit Card + Gift Card");

    orderService.addPayment(order.getOrderId(), "CREDIT_CARD", 75.00);
    logger.info("Applied credit card payment: $75.00");

    orderService.addPayment(order.getOrderId(), "GIFT_CARD", 25.00);
    logger.info("Applied gift card payment: $25.00");

    double totalPaid = orderService.getTotalPaid(order.getOrderId());
    assertEquals(100.00, totalPaid, 0.01);

    logger.info("✓ Split payment processed successfully. Total paid: ${}", String.format("%.2f", totalPaid));
  }

  @Nested
  @DisplayName("Bulk Order Operations")
  class BulkOrderTests {

    @BeforeEach
    void setupBulkTests() {
      logger.info("→ Setting up bulk order test data");

      for (int i = 1; i <= 5; i++) {
        Order order = orderService.createOrder("BULK-CUST-" + i);
        orderService.addItem(order.getOrderId(), "BULK-ITEM-" + i, i, 10.00 * i);
      }

      logger.info("Created 5 orders for bulk testing");
    }

    @Test
    @DisplayName("✓ Should process all pending orders")
    void testProcessAllPendingOrders() {
      logger.info("▶ Testing bulk order processing");

      int processed = orderService.processAllPending();

      assertEquals(5, processed);
      logger.info("✓ Processed {} orders in bulk", processed);
    }

    @Test
    @DisplayName("✓ Should generate bulk order report")
    void testGenerateBulkReport() {
      logger.info("▶ Generating bulk order report");

      String report = orderService.generateOrderReport();

      assertNotNull(report);
      assertTrue(report.contains("Total Orders: 5"));
      logger.debug("Report:\n{}", report);
      logger.info("✓ Bulk report generated successfully");
    }
  }

  @Nested
  @DisplayName("Order Fulfillment Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("✓ Should handle empty order")
    void testEmptyOrder() {
      logger.info("▶ Testing empty order scenario");

      Order order = orderService.createOrder("CUST-EMPTY");
      logger.warn("⚠ Order {} has no items", order.getOrderId());

      double total = orderService.calculateTotal(order.getOrderId());

      assertEquals(0.00, total, 0.01);
      logger.info("✓ Empty order handled correctly with $0.00 total");
    }

    @Test
    @DisplayName("✗ Should fail for negative quantity")
    void testNegativeQuantity() {
      logger.info("▶ Testing negative quantity validation");

      Order order = orderService.createOrder("CUST-NEG");

      logger.warn("⚠ Attempting to add item with negative quantity");
      Exception exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> orderService.addItem(order.getOrderId(), "ITEM-001", -5, 50.00));

      logger.error("✗ Validation error: {}", exception.getMessage());
      assertTrue(exception.getMessage().contains("Quantity"));
    }

    @Test
    @DisplayName("✗ Should fail for order not found")
    void testOrderNotFound() {
      logger.info("▶ Testing order not found scenario");

      String fakeOrderId = "ORDER-DOES-NOT-EXIST";
      logger.warn("⚠ Attempting to access non-existent order: {}", fakeOrderId);

      Exception exception =
          assertThrows(
              NoSuchElementException.class, () -> orderService.getOrderStatus(fakeOrderId));

      logger.error("✗ Expected error: {}", exception.getMessage());
      assertTrue(exception.getMessage().contains("not found"));
    }
  }

  @AfterEach
  void teardown() {
    logger.debug("Cleaning up order test data");
    orderService = null;
  }

  @AfterAll
  static void teardownClass() {
    logger.info("╔══════════════════════════════════════════════════════════╗");
    logger.info("║   Completed Order Processing Test Suite                ║");
    logger.info("╚══════════════════════════════════════════════════════════╝");
  }

  // Order domain classes
  static class Order {
    private final String orderId;
    private final String customerId;
    private final List<OrderItem> items = new ArrayList<>();
    private final List<Payment> payments = new ArrayList<>();
    private String status = "PENDING";
    private double discount = 0.0;
    private boolean cancelled = false;

    public Order(String orderId, String customerId) {
      this.orderId = orderId;
      this.customerId = customerId;
    }

    public String getOrderId() {
      return orderId;
    }

    public String getCustomerId() {
      return customerId;
    }

    public List<OrderItem> getItems() {
      return items;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public double getDiscount() {
      return discount;
    }

    public void setDiscount(double discount) {
      this.discount = discount;
    }

    public boolean isCancelled() {
      return cancelled;
    }

    public void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
    }

    public void addPayment(Payment payment) {
      payments.add(payment);
    }

    public double getTotalPaid() {
      return payments.stream().mapToDouble(Payment::getAmount).sum();
    }
  }

  static class OrderItem {
    private final String itemId;
    private final int quantity;
    private final double price;
    private final double weight;

    public OrderItem(String itemId, int quantity, double price, double weight) {
      this.itemId = itemId;
      this.quantity = quantity;
      this.price = price;
      this.weight = weight;
    }

    public int getQuantity() {
      return quantity;
    }

    public double getPrice() {
      return price;
    }

    public double getWeight() {
      return weight;
    }

    public double getTotal() {
      return quantity * price;
    }
  }

  static class Payment {
    private final String method;
    private final double amount;

    public Payment(String method, double amount) {
      this.method = method;
      this.amount = amount;
    }

    public double getAmount() {
      return amount;
    }
  }

  static class OrderService {
    private final Map<String, Order> orders = new HashMap<>();
    private int orderCounter = 1000;

    public Order createOrder(String customerId) {
      String orderId = "ORD-" + (orderCounter++);
      Order order = new Order(orderId, customerId);
      orders.put(orderId, order);
      return order;
    }

    public void addItem(String orderId, String itemId, int quantity, double price) {
      addItemWithWeight(orderId, itemId, quantity, price, 1.0);
    }

    public void addItemWithWeight(
        String orderId, String itemId, int quantity, double price, double weight) {
      Order order = getOrder(orderId);
      if (order.isCancelled()) {
        throw new IllegalStateException("Cannot add items to cancelled order");
      }
      if (quantity <= 0) {
        throw new IllegalArgumentException("Quantity must be positive");
      }
      order.getItems().add(new OrderItem(itemId, quantity, price, weight));
    }

    public double calculateTotal(String orderId) {
      Order order = getOrder(orderId);
      double subtotal =
          order.getItems().stream().mapToDouble(OrderItem::getTotal).sum();
      return subtotal - order.getDiscount();
    }

    public boolean applyDiscount(String orderId, String code) {
      if (!code.equals("SAVE20")) {
        throw new IllegalArgumentException("Invalid discount code: " + code);
      }
      Order order = getOrder(orderId);
      double subtotal =
          order.getItems().stream().mapToDouble(OrderItem::getTotal).sum();
      order.setDiscount(subtotal * 0.20);
      return true;
    }

    public void updateStatus(String orderId, String status) {
      getOrder(orderId).setStatus(status);
    }

    public String getOrderStatus(String orderId) {
      return getOrder(orderId).getStatus();
    }

    public void cancelOrder(String orderId) {
      Order order = getOrder(orderId);
      order.setCancelled(true);
      order.setStatus("CANCELLED");
    }

    public double calculateShipping(String orderId) {
      Order order = getOrder(orderId);
      double totalWeight =
          order.getItems().stream().mapToDouble(item -> item.getWeight() * item.getQuantity()).sum();
      return Math.max(5.00, totalWeight * 2.0);
    }

    public void addPayment(String orderId, String method, double amount) {
      getOrder(orderId).addPayment(new Payment(method, amount));
    }

    public double getTotalPaid(String orderId) {
      return getOrder(orderId).getTotalPaid();
    }

    public int processAllPending() {
      int count = 0;
      for (Order order : orders.values()) {
        if (order.getStatus().equals("PENDING")) {
          order.setStatus("PROCESSED");
          count++;
        }
      }
      return count;
    }

    public String generateOrderReport() {
      StringBuilder report = new StringBuilder("ORDER REPORT\n");
      report.append("============\n");
      report.append("Total Orders: ").append(orders.size()).append("\n");
      return report.toString();
    }

    private Order getOrder(String orderId) {
      Order order = orders.get(orderId);
      if (order == null) {
        throw new NoSuchElementException("Order " + orderId + " not found");
      }
      return order;
    }
  }
}
