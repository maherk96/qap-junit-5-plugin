package com.example.testapp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisplayName("Inventory Service Tests")
@Tag("inventory")
@Tag("warehouse")
class InventoryServiceTest {

  private static final Logger logger = LoggerFactory.getLogger(InventoryServiceTest.class);
  private InventoryService inventoryService;

  @BeforeAll
  static void setupClass() {
    logger.info("=== Starting Inventory Service Test Suite ===");
    logger.info("Connecting to inventory database");
  }

  @BeforeEach
  void setup() {
    logger.info("Initializing inventory service");
    inventoryService = new InventoryService();

    // Add some initial inventory
    logger.debug("Adding initial inventory items");
    inventoryService.addProduct("WIDGET-001", "Blue Widget", 100);
    inventoryService.addProduct("GADGET-002", "Red Gadget", 50);
    inventoryService.addProduct("TOOL-003", "Green Tool", 25);
    logger.info("Initial inventory setup complete - 3 products added");
  }

  @Test
  @DisplayName("Should add new product to inventory")
  void testAddProduct() {
    logger.info("Testing add new product");

    String sku = "NEW-PROD-001";
    String name = "New Product";
    int quantity = 75;

    logger.debug("Adding product: SKU={}, Name={}, Qty={}", sku, name, quantity);

    boolean added = inventoryService.addProduct(sku, name, quantity);

    assertTrue(added);
    assertEquals(quantity, inventoryService.getStock(sku));
    logger.info("Product added successfully: {}", sku);
  }

  @Test
  @DisplayName("Should update product stock")
  void testUpdateStock() {
    logger.info("Testing stock update");

    String sku = "WIDGET-001";
    int newQuantity = 150;

    logger.debug("Updating stock for {} to {}", sku, newQuantity);

    inventoryService.updateStock(sku, newQuantity);

    assertEquals(newQuantity, inventoryService.getStock(sku));
    logger.info("Stock updated: {} now has {} units", sku, newQuantity);
  }

  @Test
  @DisplayName("Should reserve inventory for order")
  void testReserveInventory() {
    logger.info("Testing inventory reservation");

    String sku = "WIDGET-001";
    int reserveQty = 10;
    int originalStock = inventoryService.getStock(sku);

    logger.debug("Reserving {} units of {} (current stock: {})", reserveQty, sku, originalStock);

    boolean reserved = inventoryService.reserveStock(sku, reserveQty);

    assertTrue(reserved);
    assertEquals(originalStock - reserveQty, inventoryService.getStock(sku));
    logger.info("Reserved {} units of {}, remaining stock: {}", reserveQty, sku, originalStock - reserveQty);
  }

  @Test
  @DisplayName("Should fail to reserve more than available stock")
  void testReserveExceedsStock() {
    logger.info("Testing over-reservation scenario");

    String sku = "GADGET-002";
    int currentStock = inventoryService.getStock(sku);
    int attemptReserve = currentStock + 50;

    logger.warn(
        "Attempting to reserve {} units when only {} available", attemptReserve, currentStock);

    Exception exception =
        assertThrows(
            IllegalStateException.class, () -> inventoryService.reserveStock(sku, attemptReserve));

    logger.error("Reservation failed as expected: {}", exception.getMessage());
    assertTrue(exception.getMessage().contains("Insufficient"));
  }

  @Test
  @DisplayName("Should check low stock items")
  void testLowStockAlert() {
    logger.info("Testing low stock detection");

    int threshold = 30;
    logger.debug("Checking for items below threshold: {}", threshold);

    List<String> lowStockItems = inventoryService.getLowStockItems(threshold);

    assertFalse(lowStockItems.isEmpty());
    logger.warn("Found {} items with low stock", lowStockItems.size());
    lowStockItems.forEach(sku -> logger.warn("Low stock alert: {} has {} units", sku, inventoryService.getStock(sku)));
  }

  @Test
  @DisplayName("Should calculate total inventory value")
  void testInventoryValue() {
    logger.info("Testing inventory value calculation");

    // Set prices for test
    inventoryService.setPrice("WIDGET-001", 10.00);
    inventoryService.setPrice("GADGET-002", 25.00);
    inventoryService.setPrice("TOOL-003", 50.00);

    logger.debug("Calculating total inventory value");

    double totalValue = inventoryService.calculateTotalValue();

    assertTrue(totalValue > 0);
    logger.info("Total inventory value: ${}", String.format("%.2f", totalValue));
  }

  @Test
  @DisplayName("Should handle product not found")
  void testProductNotFound() {
    logger.info("Testing product not found scenario");

    String nonExistentSku = "DOES-NOT-EXIST";
    logger.warn("Attempting to get stock for non-existent SKU: {}", nonExistentSku);

    Exception exception =
        assertThrows(NoSuchElementException.class, () -> inventoryService.getStock(nonExistentSku));

    logger.error("Expected error: {}", exception.getMessage());
    assertTrue(exception.getMessage().contains("not found"));
  }

  @Nested
  @DisplayName("Batch Operations")
  class BatchOperationTests {

    @Test
    @DisplayName("Should process bulk stock update")
    void testBulkStockUpdate() {
      logger.info("Testing bulk stock update");

      Map<String, Integer> updates = new HashMap<>();
      updates.put("WIDGET-001", 200);
      updates.put("GADGET-002", 75);
      updates.put("TOOL-003", 50);

      logger.debug("Processing bulk update for {} items", updates.size());

      int updated = inventoryService.bulkUpdateStock(updates);

      assertEquals(3, updated);
      logger.info("Bulk update completed: {} items updated", updated);
    }

    @Test
    @DisplayName("Should generate inventory report")
    void testGenerateReport() {
      logger.info("Generating inventory report");

      String report = inventoryService.generateReport();

      assertNotNull(report);
      assertTrue(report.contains("WIDGET-001"));
      logger.info("Report generated successfully");
      logger.debug("Report content:\n{}", report);
    }
  }

  @AfterEach
  void teardown() {
    logger.info("Cleaning up inventory test data");
    inventoryService = null;
  }

  @AfterAll
  static void teardownClass() {
    logger.info("=== Completed Inventory Service Test Suite ===");
    logger.info("Disconnecting from inventory database");
  }

  // Inventory domain classes
  static class Product {
    private final String sku;
    private final String name;
    private int quantity;
    private double price;

    public Product(String sku, String name, int quantity) {
      this.sku = sku;
      this.name = name;
      this.quantity = quantity;
      this.price = 0.0;
    }

    public String getSku() {
      return sku;
    }

    public String getName() {
      return name;
    }

    public int getQuantity() {
      return quantity;
    }

    public void setQuantity(int quantity) {
      this.quantity = quantity;
    }

    public double getPrice() {
      return price;
    }

    public void setPrice(double price) {
      this.price = price;
    }
  }

  static class InventoryService {
    private final Map<String, Product> inventory = new HashMap<>();

    public boolean addProduct(String sku, String name, int quantity) {
      if (inventory.containsKey(sku)) {
        return false;
      }
      inventory.put(sku, new Product(sku, name, quantity));
      return true;
    }

    public void updateStock(String sku, int quantity) {
      Product product = getProduct(sku);
      product.setQuantity(quantity);
    }

    public int getStock(String sku) {
      return getProduct(sku).getQuantity();
    }

    public boolean reserveStock(String sku, int quantity) {
      Product product = getProduct(sku);
      if (product.getQuantity() < quantity) {
        throw new IllegalStateException(
            "Insufficient stock for " + sku + ". Available: " + product.getQuantity());
      }
      product.setQuantity(product.getQuantity() - quantity);
      return true;
    }

    public void setPrice(String sku, double price) {
      getProduct(sku).setPrice(price);
    }

    public List<String> getLowStockItems(int threshold) {
      return inventory.values().stream()
          .filter(p -> p.getQuantity() < threshold)
          .map(Product::getSku)
          .collect(java.util.stream.Collectors.toList());
    }

    public double calculateTotalValue() {
      return inventory.values().stream()
          .mapToDouble(p -> p.getQuantity() * p.getPrice())
          .sum();
    }

    public int bulkUpdateStock(Map<String, Integer> updates) {
      int count = 0;
      for (Map.Entry<String, Integer> entry : updates.entrySet()) {
        try {
          updateStock(entry.getKey(), entry.getValue());
          count++;
        } catch (NoSuchElementException e) {
          // Skip invalid SKUs
        }
      }
      return count;
    }

    public String generateReport() {
      StringBuilder report = new StringBuilder("INVENTORY REPORT\n");
      report.append("================\n");
      for (Product p : inventory.values()) {
        report
            .append(String.format("%s - %s: %d units @ $%.2f\n", p.getSku(), p.getName(), p.getQuantity(), p.getPrice()));
      }
      return report.toString();
    }

    private Product getProduct(String sku) {
      Product product = inventory.get(sku);
      if (product == null) {
        throw new NoSuchElementException("Product " + sku + " not found");
      }
      return product;
    }
  }
}
