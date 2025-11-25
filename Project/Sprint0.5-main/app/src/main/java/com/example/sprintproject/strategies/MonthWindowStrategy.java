// This class filters the expenses based on their date data to expenses that occur
// within a month time period.
// It configures the query used by the expense window
// system so that the charts only show the relevant data for the
// set month time period.

package com.example.sprintproject.strategies;

import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.viewmodel.FirestoreManager;
import com.google.firebase.firestore.Query;

import java.util.Calendar;

public class MonthWindowStrategy extends ExpenseWindowStrategy {
    private final AppDate date;

    public MonthWindowStrategy(AppDate date) {
        this.date = date;
    }

    @Override
    protected Query windowQuery(FirestoreManager fm, String uid) {
        Calendar start = Calendar.getInstance();
        start.set(date.getYear(), date.getMonth() - 1, 1, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);
        long startMs = start.getTimeInMillis();

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        long endMs = end.getTimeInMillis();

        return fm.expensesReference(uid)
                .whereGreaterThanOrEqualTo("timestamp", startMs)
                .whereLessThan("timestamp", endMs);
    }
}
