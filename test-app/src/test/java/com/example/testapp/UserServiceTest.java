package com.example.testapp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisplayName("User Service Tests")
@Tag("user-service")
@Tag("integration")
class UserServiceTest {

  private static final Logger logger = LoggerFactory.getLogger(UserServiceTest.class);
  private UserService userService;

  @BeforeAll
  static void setupClass() {
    logger.info("=== Starting User Service Test Suite ===");
    logger.debug("Initializing test environment");
  }

  @BeforeEach
  void setup() {
    logger.info("Setting up test case");
    userService = new UserService();
    logger.debug("UserService instance created: {}", userService);
  }

  @AfterEach
  void teardown() {
    logger.info("Cleaning up test case");
    userService = null;
  }

  @AfterAll
  static void teardownClass() {
    logger.info("=== Completed User Service Test Suite ===");
  }

  @Test
  @DisplayName("Should create new user successfully")
  void testCreateUser() {
    logger.info("Testing user creation");
    String username = "john.doe";
    String email = "john.doe@example.com";

    logger.debug("Creating user with username: {}, email: {}", username, email);
    User user = userService.createUser(username, email);

    assertNotNull(user, "User should not be null");
    assertEquals(username, user.getUsername());
    assertEquals(email, user.getEmail());
    logger.info("User created successfully: {}", user);
  }

  @Test
  @DisplayName("Should find user by username")
  void testFindUserByUsername() {
    logger.info("Testing user lookup by username");

    // Create a user first
    String username = "jane.smith";
    userService.createUser(username, "jane.smith@example.com");
    logger.debug("User created for lookup test: {}", username);

    // Find the user
    User found = userService.findByUsername(username);

    assertNotNull(found, "User should be found");
    assertEquals(username, found.getUsername());
    logger.info("User found successfully: {}", found);
  }

  @Test
  @DisplayName("Should update user email")
  void testUpdateUserEmail() {
    logger.info("Testing user email update");

    // Create user
    String username = "bob.jones";
    String oldEmail = "bob.old@example.com";
    String newEmail = "bob.new@example.com";

    User user = userService.createUser(username, oldEmail);
    logger.debug("Created user with email: {}", oldEmail);

    // Update email
    logger.debug("Updating email to: {}", newEmail);
    userService.updateEmail(username, newEmail);

    User updated = userService.findByUsername(username);
    assertEquals(newEmail, updated.getEmail());
    logger.info("Email updated successfully from {} to {}", oldEmail, newEmail);
  }

  @Test
  @DisplayName("Should delete user")
  void testDeleteUser() {
    logger.info("Testing user deletion");

    String username = "temp.user";
    userService.createUser(username, "temp@example.com");
    logger.debug("Created temporary user: {}", username);

    boolean deleted = userService.deleteUser(username);

    assertTrue(deleted, "User should be deleted");
    assertNull(userService.findByUsername(username), "User should not exist after deletion");
    logger.info("User deleted successfully: {}", username);
  }

  @Test
  @DisplayName("Should throw exception for duplicate username")
  void testDuplicateUsername() {
    logger.info("Testing duplicate username handling");

    String username = "duplicate.user";
    userService.createUser(username, "user1@example.com");
    logger.debug("Created first user with username: {}", username);

    logger.warn("Attempting to create duplicate user");
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> userService.createUser(username, "user2@example.com"));

    logger.error("Expected exception caught: {}", exception.getMessage());
    assertTrue(exception.getMessage().contains("already exists"));
  }

  @Test
  @DisplayName("Should validate email format")
  void testEmailValidation() {
    logger.info("Testing email validation");

    String invalidEmail = "invalid-email";
    logger.warn("Testing with invalid email: {}", invalidEmail);

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> userService.createUser("test.user", invalidEmail));

    logger.error("Validation error: {}", exception.getMessage());
    assertTrue(exception.getMessage().contains("Invalid email"));
  }

  @Nested
  @DisplayName("User Search Operations")
  class UserSearchTests {

    @BeforeEach
    void setupSearchTests() {
      logger.info("Setting up search test data");
      userService.createUser("alice", "alice@example.com");
      userService.createUser("bob", "bob@example.com");
      userService.createUser("charlie", "charlie@example.com");
      logger.debug("Created 3 test users");
    }

    @Test
    @DisplayName("Should list all users")
    void testListAllUsers() {
      logger.info("Testing list all users");

      var users = userService.listAllUsers();

      assertEquals(3, users.size());
      logger.info("Found {} users", users.size());
    }

    @Test
    @DisplayName("Should search users by email domain")
    void testSearchByDomain() {
      logger.info("Testing search by email domain");

      String domain = "@example.com";
      logger.debug("Searching for users with domain: {}", domain);

      var users = userService.searchByEmailDomain(domain);

      assertEquals(3, users.size());
      logger.info("Found {} users with domain {}", users.size(), domain);
    }
  }

  // Simple User class for testing
  static class User {
    private final String username;
    private String email;

    public User(String username, String email) {
      this.username = username;
      this.email = email;
    }

    public String getUsername() {
      return username;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    @Override
    public String toString() {
      return "User{username='" + username + "', email='" + email + "'}";
    }
  }

  // Simple UserService for testing
  static class UserService {
    private final java.util.Map<String, User> users = new java.util.HashMap<>();

    public User createUser(String username, String email) {
      if (!email.contains("@")) {
        throw new IllegalArgumentException("Invalid email format: " + email);
      }
      if (users.containsKey(username)) {
        throw new IllegalArgumentException("User " + username + " already exists");
      }
      User user = new User(username, email);
      users.put(username, user);
      return user;
    }

    public User findByUsername(String username) {
      return users.get(username);
    }

    public void updateEmail(String username, String newEmail) {
      User user = users.get(username);
      if (user != null) {
        user.setEmail(newEmail);
      }
    }

    public boolean deleteUser(String username) {
      return users.remove(username) != null;
    }

    public java.util.List<User> listAllUsers() {
      return new java.util.ArrayList<>(users.values());
    }

    public java.util.List<User> searchByEmailDomain(String domain) {
      return users.values().stream()
          .filter(u -> u.getEmail().contains(domain))
          .collect(java.util.stream.Collectors.toList());
    }
  }
}
