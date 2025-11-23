package com.example.sprintproject.viewmodel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;
import java.util.concurrent.TimeUnit;

public class ExpenseRepository {

    private static final String TIMESTAMP_FIELD = "timestamp";

    /**
     * Calculates the number of full days between two timestamps.
     * @param dateMillis The older timestamp
     * @param todayMillis The newer timestamp
     * @return the day diff.
     */
    public static int calculateDaysSince(long dateMillis, long todayMillis) {
        if (dateMillis == 0) {
            return -1;
        }

        long oneDayMillis = TimeUnit.DAYS.toMillis(1);
        long startOfDayDate = (dateMillis / oneDayMillis) * oneDayMillis;
        long startOfDayToday = (todayMillis / oneDayMillis) * oneDayMillis;

        if (startOfDayToday <= startOfDayDate) {
            return 0;
        }

        long diff = startOfDayToday - startOfDayDate;
        // divide to get how many days
        return (int) (diff / oneDayMillis);
    }

    /**
     * Gets the timestamp of the last recorded expense for the current user.
     *
     * @param callback The callback to execute with the result.
     */
    public void getLastExpenseLogDate(LastLogCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            callback.onLastLogDateRetrieved(0);
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        FirestoreManager.getInstance().expensesReference(uid)
                .orderBy(TIMESTAMP_FIELD, Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onLastLogDateRetrieved(0);
                    } else {
                        Long timestamp = querySnapshot.getDocuments().get(0)
                                .getLong(TIMESTAMP_FIELD);
                        long result = timestamp != null ? timestamp : 0;
                        callback.onLastLogDateRetrieved(result);
                    }
                })
                .addOnFailureListener(e -> callback.onLastLogDateRetrieved(0));
    }

    public interface LastLogCallback {
        /**
         * @param lastLogMillis The timestamp of the last logged expense, or 0 if none found.
         */
        void onLastLogDateRetrieved(long lastLogMillis);
    }
}