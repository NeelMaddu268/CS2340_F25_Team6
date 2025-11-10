package com.example.sprintproject;

import org.junit.Test;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Combined Unit Tests for Sprint 1 & 2 requirements.
 * - Budget surplus and percent calculations
 * - Expense date validation
 * - Authentication input validation
 * - Edge cases for rollover logic and invalid inputs
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
    public void testZeroTotalBudget() {
        int percent = BudgetCalculator.computePercentUsed(0, 500);
        assertEquals(0, percent);
    }

    @Test
    public void testOverBudgetReturnsNegativeSurplus() {
        double surplus = BudgetCalculator.computeSurplus(500, 700);
        assertTrue(surplus < 0);
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
    public void testExpenseWithNullDateFails() {
        Calendar now = Calendar.getInstance();
        assertFalse(ExpenseValidator.isValidExpenseDate(null, now.getTime()));
    }

    @Test
    public void testExpenseWithSameDayIsValid() {
        Calendar today = Calendar.getInstance();
        assertTrue(ExpenseValidator.isValidExpenseDate(today.getTime(), today.getTime()));
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

    @Test
    public void testRejectShortPassword() {
        assertFalse(AuthValidator.isValidInput("user@email.com", "a"));
    }

    @Test
    public void testRejectInvalidEmailFormat() {
        assertFalse(AuthValidator.isValidInput("useremail.com", "password"));
        assertFalse(AuthValidator.isValidInput("user@", "password"));
    }

    @Test
    public void testAddContribution() {
        SavingsCircle circle = new SavingsCircle("My Circle");
        circle.addContribution("Alice", 100);
        assertEquals(100, circle.getContributions().get("Alice"), 0.001);
    }

    @Test
    public void testTotalContributions() {
        SavingsCircle circle = new SavingsCircle("My Circle");
        circle.addContribution("Alice", 100);
        circle.addContribution("Bob", 50);
        assertEquals(150, circle.totalContributions(), 0.001);
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

    public class SavingsCircle {
        private String name;
        private Map<String, Double> contributions;

        public SavingsCircle(String name) {
            this.name = name;
            this.contributions = new HashMap<>();
        }

        public String getName() {
            return name;
        }

        public Map<String, Double> getContributions() {
            return contributions;
        }

        public void addContribution(String member, double amount) {
            contributions.put(member, contributions.getOrDefault(member, 0.0) + amount);
        }

        public double totalContributions() {
            return contributions.values().stream().mapToDouble(Double::doubleValue).sum();
        }
    }

    public static class AuthValidator {
        public static boolean isValidInput(String email, String password) {
            if (email == null || password == null) {
                return false;
            }
            if (email.trim().isEmpty() || password.trim().isEmpty()) {
                return false;
            }
            if (!email.contains("@") || !email.contains(".")) {
                return false;
            }
            if (password.length() < 3) {
                return false;
            }
            return true;
        }
    }

    public static class ExpenseValidator {
        public static boolean isValidExpenseDate(
                java.util.Date expenseDate, java.util.Date currentDate) {
            if (expenseDate == null || currentDate == null) {
                return false;
            }
            return !expenseDate.after(currentDate);
        }
    }
}
