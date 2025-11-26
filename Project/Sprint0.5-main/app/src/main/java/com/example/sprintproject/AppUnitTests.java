package com.example.sprintproject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import java.util.*;

import com.example.sprintproject.model.ExpenseData;
import com.example.sprintproject.model.NotificationData;
import com.example.sprintproject.viewmodel.ExpenseCreationViewModel;
import com.example.sprintproject.viewmodel.ExpenseRepository;
import com.example.sprintproject.viewmodel.NotificationQueueManager;
import com.example.sprintproject.logic.FinancialInsightsEngine;


public class AppUnitTests {

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

    @Test
    public void testAcceptValidSavingsCircleInputs() {
        assertTrue(SavingsCircleValidator.isValidInput("Group Name", "Challenge Title",
                500, "monthly"));
    }

    @Test
    public void testRejectInvalidSavingsCircleInputs() {
        assertFalse(SavingsCircleValidator.isValidInput("", "Challenge Title",
                100, "monthly"));
        assertFalse(SavingsCircleValidator.isValidInput("Group Name", "Challenge Title",
                -50, "weekly"));
        assertFalse(SavingsCircleValidator.isValidInput("Group Name", "Challenge Title",
                100, "daily"));
    }

    @Test
    public void testAddAndRemoveBudget() {
        User user = new User("john@example.com", "John Doe", "password123",
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
        User user = new User("john@example.com", "John Doe", "password123",
                new ArrayList<>(), new ArrayList<>());
        Expense expense1 = new Expense("Dinner");
        user.addExpense(expense1);
        assertEquals(1, user.getExpenses().size());

        user.removeExpense(expense1);
        assertTrue(user.getExpenses().isEmpty());
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
            return password.length() >= 3;
        }
    }

    public static class ExpenseValidator {
        public static boolean isValidExpenseDate(Date expenseDate, Date currentDate) {
            if (expenseDate == null || currentDate == null) {
                return false;
            }
            return !expenseDate.after(currentDate);
        }
    }

    public static class SavingsCircleValidator {
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
            if (frequency == null || !frequency.equals("weekly") && !frequency.equals("monthly")) {
                return false;
            }
            return true;
        }
    }

    @Test
    public void testExpenseContributionToSavingsCircle() {
        SavingsCircle testCircle = new SavingsCircle("Test Circle");
        testCircle.addContribution("Alice", 100);
        assertEquals(100, testCircle.getContributions().get("Alice"), 0.001);
        testCircle.addContribution("Alice", 50);
        assertEquals(150, testCircle.getContributions().get("Alice"), 0.001);
    }

    @Test
    public void testExpenseTowardsGroupContribution() {
        ExpenseData data = new ExpenseData(
                "Toys", "11/25/2025", "15.00",
                "eating", "NA", true, "testCircle"
        );

        assertTrue(data.getContributesToGroupSavings());
        assertEquals("testCircle", data.getCircleId());
    }

    @Test
    public void testExpenseTowardsNoGroupContribution() {
        ExpenseData data = new ExpenseData(
                "Toys", "11/25/2025", "15.00",
                "eating", "NA", false, null
        );

        assertFalse(data.getContributesToGroupSavings());
        assertNull(data.getCircleId());
    }

    @Test
    public void testBudgetReminder() {
        NotificationData warning = NotificationData.createAlmostBudgetFullReminder("Test", 99);
        assertEquals(NotificationData.Type.BUDGET_WARNING, warning.getType());
        assertEquals("Budget Almost Full Warning", warning.getTitle());
        assertTrue(warning.getPriority() >= 80);
        assertTrue(warning.getMessage().contains("99"));
    }

    @Test
    public void testCalcDaysSinceLastLogZero() {
        long zero = System.currentTimeMillis();
        assertEquals(0, ExpenseRepository.calculateDaysSince(zero, zero));

    }

    @Test
    public void testCalcDaysIfNoLogs() {
        long none = System.currentTimeMillis();
        int result = ExpenseRepository.calculateDaysSince(0, none);
        assertEquals(-1, result);
    }

    @Test
    public void testChatbotWeeklySummaryHandled() {
        FinancialInsightsEngine engine = new FinancialInsightsEngine();

        FinancialInsightsEngine.InsightResult result =
                engine.tryHandle("Please summarize my spending this week",
                        Collections.emptyList(), Collections.emptyList());

        assertTrue(result.handled);
        assertNotNull(result.computedText);
        assertNotNull(result.aiFollowupPrompt);
        assertTrue(result.computedText.contains("Spending last 7 days"));
    }

    @Test
    public void testChatbotWeeklySummaryPromptContainsComputedText() {
        FinancialInsightsEngine engine = new FinancialInsightsEngine();

        FinancialInsightsEngine.InsightResult result =
                engine.tryHandle("summarize my spending this week",
                        Collections.emptyList(), Collections.emptyList());

        assertTrue(result.handled);
        assertNotNull(result.aiFollowupPrompt);
        assertTrue(result.aiFollowupPrompt.contains(result.computedText));
    }

    @Test
    public void testChatbotCutCostsHandled() {
        FinancialInsightsEngine engine = new FinancialInsightsEngine();

        FinancialInsightsEngine.InsightResult result =
                engine.tryHandle("Can you suggest where I can cut costs?",
                        Collections.emptyList(), Collections.emptyList());

        assertTrue(result.handled);
        assertNotNull(result.computedText);
        assertNotNull(result.aiFollowupPrompt);
        assertTrue(result.aiFollowupPrompt.toLowerCase(Locale.US)
                .contains("cut costs"));
    }

    @Test
    public void testChatbotComparedToLastMonthHandled() {
        FinancialInsightsEngine engine = new FinancialInsightsEngine();

        FinancialInsightsEngine.InsightResult result =
                engine.tryHandle("How did I perform compared to last month?",
                        Collections.emptyList(), Collections.emptyList());

        assertTrue(result.handled);
        assertNotNull(result.computedText);
        assertNotNull(result.aiFollowupPrompt);
        assertTrue(result.computedText.contains("This month: $"));
        assertTrue(result.computedText.contains("Last month: $"));
    }

    @Test
    public void testChatbotUnrelatedQuestionNotHandled() {
        FinancialInsightsEngine engine = new FinancialInsightsEngine();

        FinancialInsightsEngine.InsightResult result =
                engine.tryHandle("What is your favorite color?",
                        Collections.emptyList(), Collections.emptyList());

        assertFalse(result.handled);
        assertNull(result.computedText);
        assertNull(result.aiFollowupPrompt);
    }

    @Test
    public void testChatbotWeeklySummaryIsCaseInsensitive() {
        FinancialInsightsEngine engine = new FinancialInsightsEngine();

        FinancialInsightsEngine.InsightResult result =
                engine.tryHandle("SuMmArIzE My SpEnDiNg ThIs WeEk",
                        Collections.emptyList(), Collections.emptyList());

        assertTrue(result.handled);
        assertNotNull(result.computedText);
        assertTrue(result.computedText.contains("Spending last 7 days"));
    }

    @Test
    public void testChatbotCutCostsWorksWithNullExpensesAndBudgets() {
        FinancialInsightsEngine engine = new FinancialInsightsEngine();

        FinancialInsightsEngine.InsightResult result =
                engine.tryHandle("suggest where I can cut costs",
                        null, null);

        assertTrue(result.handled);
        assertNotNull(result.computedText);
        assertNotNull(result.aiFollowupPrompt);
    }

    @Test
    public void testChatbotCutCostsComputedTextHasBiggestCategories() {
        FinancialInsightsEngine engine = new FinancialInsightsEngine();

        FinancialInsightsEngine.InsightResult result =
                engine.tryHandle("suggest where I can cut costs",
                        Collections.emptyList(), Collections.emptyList());

        assertTrue(result.handled);
        assertNotNull(result.computedText);
        assertTrue(result.computedText.contains("Biggest categories this month"));
    }

    @Test
    public void testChatbotNullUserTextIsNotHandled() {
        FinancialInsightsEngine engine = new FinancialInsightsEngine();

        FinancialInsightsEngine.InsightResult result =
                engine.tryHandle(null,
                        Collections.emptyList(), Collections.emptyList());

        assertFalse(result.handled);
        assertNull(result.computedText);
        assertNull(result.aiFollowupPrompt);
    }

}