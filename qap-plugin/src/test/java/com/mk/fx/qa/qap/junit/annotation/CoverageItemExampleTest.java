package com.mk.fx.qa.qap.junit.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mk.fx.qa.qap.junit.extension.QAPJunitExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Example demonstrating the use of {@link CoverageItem} annotation for tracking requirements,
 * stories, or test case IDs at the method level.
 *
 * <p>Multiple coverage items can be added to a single test method by repeating the annotation.
 */
@ExtendWith(QAPJunitExtension.class)
@DisplayName("User Authentication Tests")
class CoverageItemExampleTest {

  @Test
  @CoverageItem("TC-LOGIN-001")
  @DisplayName("User can login with valid credentials")
  void testValidLogin() {
    assertEquals(1, 1);
  }

  @Test
  @CoverageItem("TC-LOGIN-002")
  @CoverageItem("BUG-1234")
  @DisplayName("Login fails with invalid credentials")
  void testInvalidLogin() {
    // This test covers both the test case TC-LOGIN-002 and bug fix BUG-1234
    assertEquals(1, 1);
  }

  @Test
  @CoverageItem("TC-LOGIN-003")
  @CoverageItem("STORY-456")
  @CoverageItem("REQ-AUTH-200")
  @DisplayName("Password reset flow")
  void testPasswordReset() {
    // This test covers multiple requirements, stories, and test cases
    assertEquals(1, 1);
  }

  @Test
  @DisplayName("Session timeout")
  void testSessionTimeout() {
    // No coverage items - this is fine, not all tests need explicit coverage tracking
    assertEquals(1, 1);
  }

  /**
   * Nested test classes work the same way - coverage items are only tracked at method level.
   */
  @DisplayName("Two-Factor Authentication Tests")
  class TwoFactorAuthenticationTests {

    @Test
    @CoverageItem("TC-2FA-001")
    @DisplayName("2FA setup for new user")
    void test2FASetup() {
      assertEquals(1, 1);
    }

    @Test
    @CoverageItem("TC-2FA-002")
    @CoverageItem("REQ-SEC-100")
    @DisplayName("2FA verification")
    void test2FAVerification() {
      // This test will have coverageItems: ["TC-2FA-002", "REQ-SEC-100"]
      assertEquals(1, 1);
    }
  }
}
