// This is an abstract class that defines the logic that is used to load the expense and budgets
// within a chosen period of time and updating the charts accordingly. It helps build database queries and allows other subclasses
// to choose which expenses are included in the result.


package com.example.sprintproject.strategies;

import com.example.sprintproject.charts.BarChartController;
import com.example.sprintproject.charts.PieChartController;
import com.example.sprintproject.viewmodel.FirestoreManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class ExpenseWindowStrategy {
    private static final String AMT_STRING = "amount";

    protected Query windowQuery(FirestoreManager fm, String uid) {
        return fm.expensesReference(uid);
    }

    public final void loadPie(FirestoreManager fm, String uid, PieChartController pie) {
        windowQuery(fm, uid)
                .get()
                .addOnSuccessListener(qs -> {
                    Map<String, Double> totals = new HashMap<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Double amt = readDouble(d, AMT_STRING);
                        String cat = d.getString("category");
                        if (amt == null || cat == null) {
                            continue;
                        }
                        String k = cat.trim().toLowerCase(Locale.US);
                        Double prev = totals.get(k);
                        totals.put(k, (prev == null ? 0.0 : prev) + amt);
                    }
                    pie.render(totals);
                })
                .addOnFailureListener(e -> pie.render(new HashMap<>()));
    }

    public final void loadBar(FirestoreManager fm, String uid, BarChartController bar) {
        windowQuery(fm, uid)
                .get()
                .addOnSuccessListener(expenseSnap -> {
                    double spent = 0.0;
                    for (DocumentSnapshot d : expenseSnap.getDocuments()) {
                        Double amt = readDouble(d, AMT_STRING);
                        if (amt != null) {
                            spent += amt;
                        }
                    }
                    final double spentFinal = spent;

                    fm.budgetsReference(uid)
                            .get()
                            .addOnSuccessListener(budgetSnap -> {
                                double budgetSum = 0.0;
                                for (DocumentSnapshot b : budgetSnap.getDocuments()) {
                                    Double t = coalesce(
                                            readDouble(b, "total"),
                                            readDouble(b, AMT_STRING),
                                            readDouble(b, "limit"),
                                            readDouble(b, "value"),
                                            readDouble(b, "budget")
                                    );
                                    if (t != null) {
                                        budgetSum += t;
                                    }
                                }
                                bar.render(budgetSum, spentFinal);
                            })
                            .addOnFailureListener(e -> bar.render(0.0, spentFinal));
                })
                .addOnFailureListener(e -> bar.render(0.0, 0.0));
    }

    protected static Double readDouble(DocumentSnapshot d, String field) {
        Object o = d.get(field);
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
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    @SafeVarargs
    protected static <T> T coalesce(T... vals) {
        for (T v : vals) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
