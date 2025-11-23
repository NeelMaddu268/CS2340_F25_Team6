package com.example.sprintproject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AppUnitTests {

    private static final String PASSWORD = "password";
    private static final String PASSWORD_TWO = "password123";
    private static final String ALICE = "Alice";
    private static final String TITLE = "Challenge Title";
    private static final String GROUP_NAME = "Group Name";
    private static final String MONTHLY = "monthly";

    // Shared constant for all utility classes to avoid duplicated literals
    private static final String UTILITY_CLASS_ERROR = "Utility class";

    @Test
    public void testComputeSurplusPositive() {
        double surplus = BudgetCalculator.computeSurplus(1000, 700);
        assertEquals(300, surplus, 0.001);
    }

    @Test
    public void testComputeSurplusZero() {
        double surplus = BudgetCalculator.computeSurplus(500, 500);
        assertEquals(0, surplus, 0.001);
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
        assertFalse(AuthValidator.isValidInput(null, PASSWORD));
    }

    @Test
    public void testAcceptValidCredentials() {
        assertTrue(AuthValidator.isValidInput("user@email.com", PASSWORD_TWO));
    }

    @Test
    public void testRejectShortPassword() {
        assertFalse(AuthValidator.isValidInput("user@email.com", "a"));
    }

    @Test
    public void testRejectInvalidEmailFormat() {
        assertFalse(AuthValidator.isValidInput("useremail.com", PASSWORD));
        assertFalse(AuthValidator.isValidInput("user@", PASSWORD));
    }

    // Mirrors your DashboardFragment.readDouble() behavior without Firebase.
    private static Double readDoubleLikeHelper(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Double) {
            return (Double) o;
        }
        if (o instanceof Long) {
            return ((Long) o).doubleValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).doubleValue();
        }
        if (o instanceof Float) {
            return ((Float) o).doubleValue();
        }
        if (o instanceof String) {
            try {
                return Double.parseDouble((String) o);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @SafeVarargs
    private static <T> T coalesce(T... vals) {
        for (T v : vals) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    @Test
    public void testReadDoubleLikeHelperparsesNumericsAndStrings() {
        assertEquals(12.5, readDoubleLikeHelper(12.5d), 1e-9);
        assertEquals(12.0, readDoubleLikeHelper(12L), 1e-9);
        assertEquals(7.0, readDoubleLikeHelper(7), 1e-9);
        assertEquals(2.5, readDoubleLikeHelper(2.5f), 1e-6);
        assertEquals(99.75, readDoubleLikeHelper("99.75"), 1e-9);
        assertEquals(42.0, readDoubleLikeHelper("42"), 1e-9);
        assertNull(readDoubleLikeHelper("hello"));
        assertNull(readDoubleLikeHelper(null));
    }

    @Test
    public void testAggregationtotalsForBarChartareCorrect() {
        List<Object> expenseAmounts = Arrays.asList(10, 20L, 7.5f, 12.25d, "50.0", "bad");
        double spent = 0.0;
        for (Object a : expenseAmounts) {
            Double v = readDoubleLikeHelper(a);
            if (v != null) {
                spent += v;
            }
        }
        assertEquals(99.75, spent, 1e-6);

        List<Map<String, Object>> budgets = new ArrayList<>();
        Map<String, Object> b1 = new HashMap<>();
        b1.put("total", 100);
        budgets.add(b1);

        Map<String, Object> b2 = new HashMap<>();
        b2.put("amount", "150.5");
        budgets.add(b2);

        Map<String, Object> b3 = new HashMap<>();
        b3.put("limit", 25L);
        budgets.add(b3);

        Map<String, Object> b4 = new HashMap<>();
        b4.put("value", 10.25f);
        budgets.add(b4);

        Map<String, Object> b5 = new HashMap<>();
        b5.put("budget", "bad");
        budgets.add(b5);

        double budgetSum = 0.0;
        for (Map<String, Object> doc : budgets) {
            Double t = coalesce(
                    readDoubleLikeHelper(doc.get("total")),
                    readDoubleLikeHelper(doc.get("amount")),
                    readDoubleLikeHelper(doc.get("limit")),
                    readDoubleLikeHelper(doc.get("value")),
                    readDoubleLikeHelper(doc.get("budget"))
            );
            if (t != null) {
                budgetSum += t;
            }
        }

        assertEquals(285.75, budgetSum, 1e-6);
        assertTrue(budgetSum > spent);
    }

    @Test
    public void testAddContribution() {
        SavingsCircle circle = new SavingsCircle("My Circle");
        circle.addContribution(ALICE, 100);
        assertEquals(100, circle.getContributions().get(ALICE), 0.001);
    }

    @Test
    public void testTotalContributions() {
        SavingsCircle circle = new SavingsCircle("My Circle");
        circle.addContribution(ALICE, 100);
        circle.addContribution("Bob", 50);
        assertEquals(150, circle.totalContributions(), 0.001);
    }

    @Test
    public void testAcceptValidSavingsCircleInputs() {
        assertTrue(SavingsCircleValidator.isValidInput(GROUP_NAME, TITLE, 500, MONTHLY));
    }

    @Test
    public void testRejectInvalidSavingsCircleInputs() {
        assertFalse(SavingsCircleValidator.isValidInput("", TITLE, 100, MONTHLY));
        assertFalse(SavingsCircleValidator.isValidInput(GROUP_NAME, TITLE, -50, "weekly"));
        assertFalse(SavingsCircleValidator.isValidInput(GROUP_NAME, TITLE, 100, "daily"));
    }

    @Test
    public void testAddAndRemoveBudget() {
        User user = new User("john@example.com", "John Doe", PASSWORD_TWO,
                new ArrayList<>(), new ArrayList<>());
        Budget budget1 = new Budget("Food Budget");
        Budget budget2 = new Budget("Travel Budget");

        user.addBudget(budget1);
        user.addBudget(budget2);

        assertEquals(2, user.getBudgets().size());
        assertTrue(user.getBudgets().contains(budget1));

        user.removeBudget(budget1);
        assertEquals(1, user.getBudgets().size());
        assertFalse(user.getBudgets().contains(budget1));
    }

    @Test
    public void testAddAndRemoveExpense() {
        User user = new User("john@example.com", "John Doe", PASSWORD_TWO,
                new ArrayList<>(), new ArrayList<>());
        Expense expense1 = new Expense("Dinner");
        user.addExpense(expense1);
        assertEquals(1, user.getExpenses().size());

        user.removeExpense(expense1);
        assertTrue(user.getExpenses().isEmpty());
    }

    // ===== Utility / Validator Classes (Sonar-compliant) =====

    public static class BudgetCalculator {
        private BudgetCalculator() {
            throw new UnsupportedOperationException(UTILITY_CLASS_ERROR);
        }

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
        private AuthValidator() {
            throw new UnsupportedOperationException(UTILITY_CLASS_ERROR);
        }

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
            return password.length() >= 3;
        }
    }

    public static class ExpenseValidator {
        private ExpenseValidator() {
            throw new UnsupportedOperationException(UTILITY_CLASS_ERROR);
        }

        public static boolean isValidExpenseDate(Date expenseDate, Date currentDate) {
            if (expenseDate == null || currentDate == null) {
                return false;
            }
            return !expenseDate.after(currentDate);
        }
    }

    public static class SavingsCircleValidator {
        private SavingsCircleValidator() {
            throw new UnsupportedOperationException(UTILITY_CLASS_ERROR);
        }

        public static boolean isValidInput(String groupName, String challengeTitle,
                                           double goalAmount, String frequency) {
            if (groupName == null || groupName.trim().isEmpty()) {
                return false;
            }
            if (challengeTitle == null || challengeTitle.trim().isEmpty()) {
                return false;
            }
            if (goalAmount < 0) {
                return false;
            }
            if (frequency == null || (!frequency.equals("weekly") && !frequency.equals(MONTHLY))) {
                return false;
            }
            return true;
        }
    }

    // ===== Simple model classes used for tests =====

    public class User {
        private String email;
        private String name;
        private String password;
        private List<Budget> budgets;
        private List<Expense> expenses;

        public User(String email, String name, String password,
                    List<Budget> budgets, List<Expense> expenses) {
            this.email = email;
            this.name = name;
            this.password = password;
            this.budgets = budgets;
            this.expenses = expenses;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public List<Budget> getBudgets() {
            return budgets;
        }

        public List<Expense> getExpenses() {
            return expenses;
        }

        public void addBudget(Budget budget) {
            if (budget != null) {
                budgets.add(budget);
            }
        }

        public void removeBudget(Budget budget) {
            budgets.remove(budget);
        }

        public void addExpense(Expense expense) {
            if (expense != null) {
                expenses.add(expense);
            }
        }

        public void removeExpense(Expense expense) {
            expenses.remove(expense);
        }
    }

    public class Budget {
        private String name;

        public Budget(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public class Expense {
        private String name;

        public Expense(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
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
            return contributions.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
        }
    }
}

