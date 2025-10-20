package com.example.sprintproject.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.sprintproject.model.Budget;
import com.example.sprintproject.model.AppDate;


import java.util.List;

public class DashboardViewModel extends ViewModel {

    private final BudgetsFragmentViewModel budgetsVM = new BudgetsFragmentViewModel();

    public LiveData<Double> getTotalSpentAllTime() {
        return budgetsVM.getTotalSpentAllTime();
    }

    public LiveData<Double> getTotalRemaining() {
        return budgetsVM.getTotalRemaining();
    }

    public void loadData() {
        budgetsVM.loadBudgets();
    }

    public LiveData<List<Budget>> getBudgetsList() {
        return budgetsVM.getBudgets();
    }

    public void loadDataFor(AppDate date) {
        budgetsVM.loadBudgetsFor(date);
    }

}