// This class keeps track of the user queries about spending patterns
// and generates financial insights about weekly summaries and etc.
// Returns AI ready prompts that the app converts into personalized messages
// back to the user.

package com.example.sprintproject.logic;

import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.Expense;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FinancialInsightsEngine {
    public InsightResult tryHandle(String userText,
                                   List<Expense> expenses,
                                   List<Budget> budgets) {
        return tryHandle(userText, expenses, budgets, Collections.emptyList());
    }

    public InsightResult tryHandle(String userText,
                                   List<Expense> expenses,
                                   List<Budget> budgets,
                                   List<?> goals) {

        if (userText == null) {
            return new InsightResult(false, null, null);
        }

        String t = userText.toLowerCase(Locale.US).trim();

        if (t.contains("summarize my spending this week")
                || t.contains("summarize my weekly spending")
                || t.contains("track my weekly expenses")
                || t.contains("weekly expenses")
                || t.contains("weekly spending")) {

            double total = sumInLastDays(expenses, 7);
            Map<String, Double> byCat = byCategoryInLastDays(expenses, 7);

            String computed = "Spending last 7 days: $" + r2(total)
                    + ". Top categories: " + topCats(byCat) + ".";

            String aiPrompt = "Here are numeric facts about the user's last 7 days of spending:\n"
                    + computed + "\n\n"
                    + "Rewrite this as a friendly 2-3 sentence summary of their weekly spending "
                    + "and add one concrete, actionable budgeting tip. "
                    + "Do NOT invent any new numbers beyond the ones provided.";

            return new InsightResult(true, computed, aiPrompt);
        }

        if (t.contains("suggest where i can cut costs")
                || t.contains("cut my costs")
                || t.contains("reduce my spending")) {

            Map<String, Double> byCat = byCategoryInLastDays(expenses, 30);

            String computed = "Biggest categories this month: " + topCats(byCat)
                    + ". Focus on reducing the top 1-2.";

            String aiPrompt =
                    "Based ONLY on these facts about the user's last 30 days of spending:\n"
                    + computed + "\n\n"
                    + "Give 3 specific, realistic suggestions for how they can cut costs. "
                    + "Don't make up any new dollar amounts.";

            return new InsightResult(true, computed, aiPrompt);
        }

        if (t.contains("compared to last month")
                || t.contains("how did i perform compared")
                || t.contains("this month vs last month")
                || t.contains("change since last month")) {

            double thisMonth = sumThisMonth(expenses);
            double lastMonth = sumLastMonth(expenses);
            double diff = thisMonth - lastMonth;

            String computed = "This month: $" + r2(thisMonth)
                    + ", Last month: $" + r2(lastMonth)
                    + " (Change: " + r2(diff) + ").";

            String aiPrompt = "Using ONLY these numeric facts:\n"
                    + computed + "\n\n"
                    + "Explain the trend in simple terms and give one practical improvement idea. "
                    + "Don't invent any additional numbers.";

            return new InsightResult(true, computed, aiPrompt);
        }

        return new InsightResult(false, null, null);
    }

    private double getAmount(Expense e) {
        return e.getAmount();
    }

    private long getTimestamp(Expense e) {
        return e.getTimestamp();
    }

    private String getCategory(Expense e) {
        return e.getCategory();
    }

    private double sumInLastDays(List<Expense> expenses, int days) {
        long now = System.currentTimeMillis();
        long cutoff = now - TimeUnit.DAYS.toMillis(days);

        double sum = 0;
        if (expenses == null) {
            return 0;
        }

        for (Expense e : expenses) {
            if (e == null) {
                continue;
            }
            long ts = getTimestamp(e);
            if (ts >= cutoff) {
                sum += getAmount(e);
            }
        }
        return sum;
    }

    private Map<String, Double> byCategoryInLastDays(List<Expense> expenses, int days) {
        long now = System.currentTimeMillis();
        long cutoff = now - TimeUnit.DAYS.toMillis(days);

        Map<String, Double> map = new HashMap<>();
        if (expenses == null) {
            return map;
        }

        for (Expense e : expenses) {
            if (e == null) {
                continue;
            }
            long ts = getTimestamp(e);
            if (ts >= cutoff) {
                String cat = getCategory(e);
                if (cat == null || cat.trim().isEmpty()) {
                    cat = "Other";
                }
                map.put(cat, map.getOrDefault(cat, 0.0) + getAmount(e));
            }
        }
        return map;
    }

    private double sumThisMonth(List<Expense> expenses) {
        if (expenses == null) {
            return 0;
        }

        Calendar cal = Calendar.getInstance();
        int m = cal.get(Calendar.MONTH);
        int y = cal.get(Calendar.YEAR);

        double sum = 0;
        for (Expense e : expenses) {
            if (e == null) {
                continue;
            }
            cal.setTimeInMillis(getTimestamp(e));
            if (cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y) {
                sum += getAmount(e);
            }
        }
        return sum;
    }

    private double sumLastMonth(List<Expense> expenses) {
        if (expenses == null) {
            return 0;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        int lastM = cal.get(Calendar.MONTH);
        int lastY = cal.get(Calendar.YEAR);

        double sum = 0;
        for (Expense e : expenses) {
            if (e == null) {
                continue;
            }
            cal.setTimeInMillis(getTimestamp(e));
            if (cal.get(Calendar.MONTH) == lastM && cal.get(Calendar.YEAR) == lastY) {
                sum += getAmount(e);
            }
        }
        return sum;
    }

    private String topCats(Map<String, Double> map) {
        if (map == null || map.isEmpty()) {
            return "None";
        }

        List<Map.Entry<String, Double>> list = new ArrayList<>(map.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(3, list.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Double> e = list.get(i);
            sb.append(e.getKey())
                    .append(" ($")
                    .append(r2(e.getValue()))
                    .append(")");
            if (i < limit - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private double r2(double x) {
        return Math.round(x * 100.0) / 100.0;
    }

    public static class InsightResult {
        private final boolean handled;
        private final String computedText;
        private final String aiFollowupPrompt;

        public InsightResult(boolean handled, String computedText, String aiFollowupPrompt) {
            this.handled = handled;
            this.computedText = computedText;
            this.aiFollowupPrompt = aiFollowupPrompt;
        }

        public String getAiFollowupPrompt() {
            return aiFollowupPrompt;
        }

        public String getComputedText() {
            return computedText;
        }

        public Boolean getHandled() {
            return handled;
        }
    }
}
