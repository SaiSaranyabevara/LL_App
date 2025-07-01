package com.example.learneasy;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LearnFragment extends Fragment {

    private String selectedLevel;

    public LearnFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_learn, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ✅ Receive Safe Args parameter
        if (getArguments() != null) {
            LearnFragmentArgs args = LearnFragmentArgs.fromBundle(getArguments());
            selectedLevel = args.getLevel();
        }

        // ✅ Set the dynamic title text like "Basic Level", "Intermediate Level"
        TextView levelTitle = view.findViewById(R.id.levelTitle);
        if (selectedLevel != null) {
            levelTitle.setText(selectedLevel + " Level");
        }

        // Reading
        View readingCard = view.findViewById(R.id.readingCard);
        readingCard.setOnClickListener(v -> startSkillActivity("Reading"));

        // Listening
        View listeningCard = view.findViewById(R.id.listeningCard);
        listeningCard.setOnClickListener(v -> startSkillActivity("Listening"));

        // Speaking
        View speakingCard = view.findViewById(R.id.speakingCard);
        speakingCard.setOnClickListener(v -> startSkillActivity("Speaking"));

        // Writing
        View writingCard = view.findViewById(R.id.writingCard);
        writingCard.setOnClickListener(v -> startSkillActivity("Writing"));
    }

    private void startSkillActivity(String skill) {
        Intent intent = new Intent(requireContext(), SkillActivity.class);
        intent.putExtra("level", selectedLevel);
        intent.putExtra("skill", skill);
        startActivity(intent);
    }
}
