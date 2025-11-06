package com.example.sprintproject.view;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.sprintproject.R;
import com.example.sprintproject.databinding.ActivityAppBinding;
import androidx.lifecycle.ViewModelProvider;
import com.example.sprintproject.viewmodel.DateViewModel;


public class AppActivity extends AppCompatActivity {

    private ActivityAppBinding binding;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DateViewModel dateVM = new ViewModelProvider(this).get(DateViewModel.class);
        dateVM.getCurrentDate().observe(this, d -> {

        });

        // Default fragment
        replaceFragment(new DashboardFragment());


        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.Dashboard) {
                replaceFragment(new DashboardFragment());
            } else if (id == R.id.Budgets) {
                replaceFragment(new BudgetsFragment());
            } else if (id == R.id.ExpenseLogs) {
                replaceFragment(new ExpensesFragment());
            } else if (id == R.id.SavingsCircles) {
                replaceFragment(new SavingsCircleFragment());
            }  else if (id == R.id.Invites) {
                replaceFragment(new InvitationsFragment());
            }  else if (id == R.id.Chatbot) {
                replaceFragment(new ChatbotFragment());
            }
            return true;
        });

    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

}
