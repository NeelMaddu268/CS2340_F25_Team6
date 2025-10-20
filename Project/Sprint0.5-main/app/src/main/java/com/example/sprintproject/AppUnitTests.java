package com.example.sprintproject;

import org.junit.Test;
import java.util.Calendar;
import static org.junit.Assert.*;

/**
 * Combined Unit Tests for Sprint 1 & 2 requirements.
 * - Covers budget surplus and percent calculations
 * - Validates expense date (no future dates)
 * - Checks input validation for account creation/login
 */
public class AppUnitTests {
    @Test
    public void testComputeSurplusPositive() {
        double surplus = BudgetCalculator.computeSurplus(1000, 700);
        assertEquals(300, surplus, 0.001);
    }

    @Test
    public void testComputePercentUsed() {
        int percent = BudgetCalculator.computePercentUsed(1000, 250);
        assertEquals(25, percent);
    }

    @Test
    public void testRejectFutureDate() {
        Calendar now = Calendar.getInstance();
        Calendar future = Calendar.getInstance();
        future.add(Calendar.DAY_OF_YEAR, 2);
        assertFalse(ExpenseValidator.isValidExpenseDate(future.getTime(), now.getTime()));
    }

    @Test
    public void testAcceptPastAndCurrentDate() {
        Calendar now = Calendar.getInstance();
        Calendar past = Calendar.getInstance();
        past.add(Calendar.DAY_OF_YEAR, -3);
        assertTrue(ExpenseValidator.isValidExpenseDate(past.getTime(), now.getTime()));
        assertTrue(ExpenseValidator.isValidExpenseDate(now.getTime(), now.getTime()));
    }

    @Test
    public void testRejectEmptyCredentials() {
        assertFalse(AuthValidator.isValidInput("", "pass"));
        assertFalse(AuthValidator.isValidInput(" ", " "));
        assertFalse(AuthValidator.isValidInput(null, "password"));
    }

    @Test
    public void testAcceptValidCredentials() {
        assertTrue(AuthValidator.isValidInput("user@email.com", "password123"));
    }

    public static class BudgetCalculator {
        public static double computeSurplus(double total, double spent) {
            return total - spent;
        }

        public static int computePercentUsed(double total, double spent) {
            if (total <= 0) {
                return 0;
            }
            return (int) ((spent / total) * 100);
        }
    }

    public static class AuthValidator {
        public static boolean isValidInput(String email, String password) {
            return email != null && password != null
                    && !email.trim().isEmpty() && !password.trim().isEmpty();
        }
    }

    public static class ExpenseValidator {
        public static boolean isValidExpenseDate(
                java.util.Date expenseDate, java.util.Date currentDate) {
            return !expenseDate.after(currentDate);
        }
    }
}
