package com.example.learneasy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnBasic = view.findViewById(R.id.btn_basic);
        Button btnIntermediate = view.findViewById(R.id.btn_intermediate);
        Button btnAdvanced = view.findViewById(R.id.btn_advanced);

        NavController navController = NavHostFragment.findNavController(this);

        btnBasic.setOnClickListener(v -> {
            if (isAdded()) {
                HomeFragmentDirections.ActionHomeFragmentToLearnFragment action =
                        HomeFragmentDirections.actionHomeFragmentToLearnFragment("Basic");
                navController.navigate(action);
            }
        });

        btnIntermediate.setOnClickListener(v -> {
            if (isAdded()) {
                HomeFragmentDirections.ActionHomeFragmentToLearnFragment action =
                        HomeFragmentDirections.actionHomeFragmentToLearnFragment("Intermediate");
                navController.navigate(action);
            }
        });

        btnAdvanced.setOnClickListener(v -> {
            if (isAdded()) {
                HomeFragmentDirections.ActionHomeFragmentToLearnFragment action =
                        HomeFragmentDirections.actionHomeFragmentToLearnFragment("Advanced");
                navController.navigate(action);
            }
        });
    }
}
