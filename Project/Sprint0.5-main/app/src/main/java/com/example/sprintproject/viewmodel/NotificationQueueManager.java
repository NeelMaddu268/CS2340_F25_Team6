package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sprintproject.model.AppDate;
import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.NotificationData;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Singleton class in charge of queueing the reminder pop-ups.
 */
public class NotificationQueueManager {

    private static NotificationQueueManager instance;

    private final PriorityQueue<NotificationData> reminderQueue = new PriorityQueue<>(
            (r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority())
    );

    private final HashMap<String, Integer> budgetWarningContainer = new HashMap<>();

    private final MutableLiveData<NotificationData> currentReminder = new MutableLiveData<>();

    private final ExpenseRepository expenseRepository;

    private NotificationQueueManager() {
        expenseRepository = new ExpenseRepository();
    }

    public static synchronized NotificationQueueManager getInstance() {
        if (instance == null) {
            instance = new NotificationQueueManager();
        }
        return instance;
    }

    public LiveData<NotificationData> getCurrentReminder() {
        return currentReminder;
    }

    /**
     * Adds new reminder to the queue.
     * @param data The notification data.
     */
    public synchronized void submitReminder(NotificationData data) {
        reminderQueue.offer(data);
        processQueue();
    }

    /**
     * Checks the queue and displays the next one if available.
     */
    private synchronized void processQueue() {
        // if an alert is already shown, don't show another yet
        if (currentReminder.getValue() == null && !reminderQueue.isEmpty()) {
            currentReminder.postValue(reminderQueue.poll());
        }
    }

    public synchronized void dismissCurrentReminder() {
        currentReminder.postValue(null);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                this::processQueue, 50
        );
    }

    public void checkForMissedExpenseLog(AppDate currentDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(currentDate.getYear(), currentDate.getMonth() - 1,
                currentDate.getDay(), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long appDateMillis = calendar.getTimeInMillis();

        expenseRepository.getLastExpenseLogDate(lastLogMillis -> {
            int daysMissed = ExpenseRepository.calculateDaysSince(lastLogMillis, appDateMillis);

            if (daysMissed > 0) {
                NotificationData reminder = NotificationData.createMissedLogReminder(daysMissed);
                submitReminder(reminder);
            }
        });
    }

    public void registerDateObserver(DateViewModel dateVM) {
        dateVM.getCurrentDate().observeForever((AppDate appDate) -> {
            if (appDate != null) {
                checkForMissedExpenseLog(appDate);
            }
        });
    }

    private boolean repeatedWarnings(String budgetId, int capacityUsed) {
        if (!budgetWarningContainer.containsKey(budgetId)) {
            return false;
        }
        return budgetWarningContainer.get(budgetId) >= capacityUsed;
    }

    private void alreadyWarned(String budgetId, int capacityUsed) {
        budgetWarningContainer.put(budgetId, capacityUsed);
    }

    /**
     * Sonar fix: reduce continues/breaks in loop to at most one.
     * (Refactored to use 0 continues.)
     */
    public void checkForBudgetWarning(List<Budget> budgets) {
        if (budgets == null || budgets.isEmpty()) {
            return;
        }

        for (Budget budget : budgets) {
            if (budget == null) {
                continue; // <= only continue in the loop
            }

            double total = budget.getAmount();
            double spent = budget.getSpentToDate();

            if (total <= 0) {
                continue;
            }

            int capacityUsed = (int) (spent / total * 100);

            boolean shouldWarn =
                    capacityUsed >= 80
                            && !repeatedWarnings(budget.getId(), capacityUsed);

            if (!shouldWarn) {
                continue;
            }

            NotificationData warning =
                    NotificationData.createAlmostBudgetFullReminder(
                            budget.getName(), capacityUsed
                    );

            submitReminder(warning);
            alreadyWarned(budget.getId(), capacityUsed);
        }
    }

}
