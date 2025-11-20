package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sprintproject.model.NotificationData;
import java.util.Calendar;
import java.util.PriorityQueue;

/**
 * Singleton class in charge of queueing the reminder pop-ups.
 * */
public class NotificationQueueManager {

    private static NotificationQueueManager instance;

    private final PriorityQueue<NotificationData> reminderQueue = new PriorityQueue<>(
            (r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority())
    );

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

    public void checkForMissedExpenseLog() {
        expenseRepository.getLastExpenseLogDate(lastLogMillis -> {

            if (lastLogMillis == 0) {
                return;
            }

            Calendar now = Calendar.getInstance();
            long todayMillis = now.getTimeInMillis();

            int daysMissed = ExpenseRepository.calculateDaysSince(lastLogMillis, todayMillis);

            if (daysMissed > 0) {
                NotificationData reminder = NotificationData.createMissedLogReminder(daysMissed);
                submitReminder(reminder);
            }
        });
    }
}