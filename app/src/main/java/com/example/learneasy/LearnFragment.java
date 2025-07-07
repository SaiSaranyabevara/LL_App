package com.example.learneasy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class LearnFragment extends Fragment {

    private String selectedLevel;

    public LearnFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_learn, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            LearnFragmentArgs args = LearnFragmentArgs.fromBundle(getArguments());
            selectedLevel = args.getLevel();
        }

        // Set level title
        TextView levelTitle = view.findViewById(R.id.levelTitle);
        if (selectedLevel != null) {
            levelTitle.setText(selectedLevel + " Level");
        }

        // Card click listeners
        view.findViewById(R.id.readingCard).setOnClickListener(v -> startSkillActivity("Reading"));
        view.findViewById(R.id.writingCard).setOnClickListener(v -> startSkillActivity("Writing"));
        view.findViewById(R.id.listeningCard).setOnClickListener(v -> startSkillActivity("Listening"));
        view.findViewById(R.id.speakingCard).setOnClickListener(v -> startSkillActivity("Speaking"));

        // Fetch progress from Firebase
        fetchSkillProgress(view);
    }

    private void startSkillActivity(String skill) {
        Intent intent = new Intent(requireContext(), SkillActivity.class);
        intent.putExtra("level", selectedLevel);
        intent.putExtra("skill", skill);
        startActivity(intent);
    }

    private void fetchSkillProgress(View view) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference quizRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("quizAnswers");

        String[] skills = {"Reading", "Writing", "Listening", "Speaking"};
        String[] levels = {"Basic", "Intermediate", "Difficult"};
        int totalLessonsPerSkill = levels.length * 6;

        for (String skill : skills) {
            int[] completedCount = {0};

            for (String level : levels) {
                quizRef.child(level).child(skill).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot lessonSnap : snapshot.getChildren()) {
                            String score = lessonSnap.child("score").getValue(String.class);
                            if (score != null && !score.startsWith("0/")) {
                                completedCount[0]++;
                            }
                        }

                        // After reading one level, update UI
                        int progressPercent = (int) ((completedCount[0] / (float) totalLessonsPerSkill) * 100);
                        updateProgressUI(view, skill, progressPercent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
            }
        }
    }

    private void updateProgressUI(View view, String skill, int percent) {
        int cardId = 0;

        switch (skill.toLowerCase()) {
            case "reading":
                cardId = R.id.readingCard;
                break;
            case "writing":
                cardId = R.id.writingCard;
                break;
            case "listening":
                cardId = R.id.listeningCard;
                break;
            case "speaking":
                cardId = R.id.speakingCard;
                break;
        }

        if (cardId != 0) {
            LinearLayout card = view.findViewById(cardId);
            CircularProgressIndicator indicator = (CircularProgressIndicator) card.getChildAt(0);
            TextView label = (TextView) card.getChildAt(1);

            indicator.setProgress(percent);
            label.setText(skill + " " + percent + "%");
        }
    }
}




/*package com.example.learneasy;

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
}*/
