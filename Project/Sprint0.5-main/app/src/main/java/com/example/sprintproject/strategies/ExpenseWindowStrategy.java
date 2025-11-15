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

    protected Query windowQuery(FirestoreManager fm, String uid) {
        return fm.expensesReference(uid);
    }

    public final void loadPie(FirestoreManager fm, String uid, PieChartController pie) {
        windowQuery(fm, uid)
                .get()
                .addOnSuccessListener(qs -> {
                    Map<String, Double> totals = new HashMap<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Double amt = readDouble(d, "amount");
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
                .addOnFailureListener(e -> pie.render(new HashMap<String, Double>()));
    }

    public final void loadBar(FirestoreManager fm, String uid, BarChartController bar) {
        windowQuery(fm, uid)
                .get()
                .addOnSuccessListener(expenseSnap -> {
                    double spent = 0.0;
                    for (DocumentSnapshot d : expenseSnap.getDocuments()) {
                        Double amt = readDouble(d, "amount");
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
                                            readDouble(b, "amount"),
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
