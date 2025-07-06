package com.example.learneasy;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class LessonDetailActivity extends AppCompatActivity {

    private TextView lessonTitle, lessonParagraph, quizScore;
    private LinearLayout quizContainer;
    private Button submitButton, retryButton;

    private final Map<String, RadioGroup> questionGroups = new HashMap<>();
    private final Map<String, String> correctAnswers = new HashMap<>();
    private final Map<String, String> userAnswers = new HashMap<>();

    private String level, skill, lessonId, uid;
    private boolean alreadyAttempted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_detail);

        lessonTitle = findViewById(R.id.lessonTitle);
        lessonParagraph = findViewById(R.id.lessonParagraph);
        quizContainer = findViewById(R.id.quizContainer);
        quizScore = findViewById(R.id.quizScore);
        submitButton = findViewById(R.id.submitButton);
        retryButton = findViewById(R.id.retryButton);
        retryButton.setVisibility(View.GONE);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        level = getIntent().getStringExtra("level");
        skill = getIntent().getStringExtra("skill");
        lessonId = getIntent().getStringExtra("lessonId");

        if (level != null && skill != null && lessonId != null) {
            checkIfAttemptedThenLoadLesson();
        } else {
            Toast.makeText(this, "Missing data", Toast.LENGTH_SHORT).show();
            finish();
        }

        submitButton.setOnClickListener(v -> {
            if (validateAllAnswered()) {
                calculateAndStoreScore();
            } else {
                Toast.makeText(this, "Please answer all questions before submitting.", Toast.LENGTH_SHORT).show();
            }
        });

        retryButton.setOnClickListener(v -> {
            quizContainer.removeAllViews();
            questionGroups.clear();
            correctAnswers.clear();
            userAnswers.clear();
            quizScore.setText("Score: 0");

            submitButton.setEnabled(true);
            submitButton.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.GONE);
            alreadyAttempted = false;

            loadLesson(false);
        });
    }

    private void checkIfAttemptedThenLoadLesson() {
        DatabaseReference userAnsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("quizAnswers")
                .child(lessonId);

        userAnsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                alreadyAttempted = snapshot.exists();
                if (alreadyAttempted) {
                    for (DataSnapshot ansSnap : snapshot.child("answers").getChildren()) {
                        userAnswers.put(ansSnap.getKey(), ansSnap.getValue(String.class));
                    }
                    String score = snapshot.child("score").getValue(String.class);
                    quizScore.setText("Score: " + score);
                    submitButton.setVisibility(View.GONE);
                    retryButton.setVisibility(View.VISIBLE);
                }
                loadLesson(alreadyAttempted);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LessonDetailActivity.this, "Failed to check attempt status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLesson(boolean readOnlyMode) {
        DatabaseReference lessonRef = FirebaseDatabase.getInstance()
                .getReference("content")
                .child(level.toLowerCase())
                .child(skill.toLowerCase())
                .child(lessonId);

        lessonRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String title = snapshot.child("title").getValue(String.class);
                String paragraph = snapshot.child("paragraph").getValue(String.class);
                lessonTitle.setText(title);
                lessonParagraph.setText(paragraph);

                quizContainer.removeAllViews();
                for (DataSnapshot quizSnap : snapshot.child("quiz").getChildren()) {
                    String quizId = quizSnap.getKey();
                    String question = quizSnap.child("question").getValue(String.class);
                    String correct = quizSnap.child("answer").getValue(String.class);
                    correctAnswers.put(quizId, correct);

                    TextView questionView = new TextView(LessonDetailActivity.this);
                    questionView.setText("Q: " + question);
                    questionView.setTextSize(16);
                    questionView.setTextColor(ContextCompat.getColor(LessonDetailActivity.this, R.color.textPrimary));
                    questionView.setPadding(0, 10, 0, 10);
                    quizContainer.addView(questionView);

                    RadioGroup group = new RadioGroup(LessonDetailActivity.this);
                    group.setOrientation(RadioGroup.VERTICAL);

                    for (DataSnapshot optionSnap : quizSnap.child("options").getChildren()) {
                        String option = optionSnap.getValue(String.class);
                        RadioButton rb = new RadioButton(LessonDetailActivity.this);
                        rb.setText(option);
                        rb.setTextColor(ContextCompat.getColor(LessonDetailActivity.this, R.color.textSecondary));

                        if (readOnlyMode && userAnswers.containsKey(quizId)) {
                            rb.setEnabled(false);
                            String selected = userAnswers.get(quizId);
                            if (rb.getText().toString().equals(selected)) {
                                if (selected.equalsIgnoreCase(correct)) {
                                    rb.setTextColor(ContextCompat.getColor(LessonDetailActivity.this, R.color.successGreen));
                                } else {
                                    rb.setTextColor(ContextCompat.getColor(LessonDetailActivity.this, R.color.errorRed));
                                }
                                rb.setChecked(true);
                            }
                        }

                        group.addView(rb);
                    }

                    quizContainer.addView(group);
                    questionGroups.put(quizId, group);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(LessonDetailActivity.this, "Failed to load lesson.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateAllAnswered() {
        for (RadioGroup group : questionGroups.values()) {
            if (group.getCheckedRadioButtonId() == -1) {
                return false;
            }
        }
        return true;
    }

    private void calculateAndStoreScore() {
        int score = 0;
        int total = correctAnswers.size();

        Map<String, String> answersToSave = new HashMap<>();

        for (Map.Entry<String, RadioGroup> entry : questionGroups.entrySet()) {
            String quizId = entry.getKey();
            RadioGroup group = entry.getValue();
            int selectedId = group.getCheckedRadioButtonId();
            RadioButton selected = findViewById(selectedId);

            String selectedText = selected.getText().toString();
            answersToSave.put(quizId, selectedText);

            selected.setChecked(true);
            selected.setEnabled(false);

            if (selectedText.equalsIgnoreCase(correctAnswers.get(quizId))) {
                selected.setTextColor(ContextCompat.getColor(this, R.color.successGreen));
                score++;
            } else {
                selected.setTextColor(ContextCompat.getColor(this, R.color.errorRed));
            }

            for (int i = 0; i < group.getChildCount(); i++) {
                group.getChildAt(i).setEnabled(false);
            }
        }

        String finalScore = score + "/" + total;
        quizScore.setText("Score: " + finalScore);

        Map<String, Object> result = new HashMap<>();
        result.put("score", finalScore);
        result.put("answers", answersToSave);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("quizAnswers")
                .child(lessonId)
                .setValue(result)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Answers saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save answers", Toast.LENGTH_SHORT).show());

        submitButton.setEnabled(false);
        submitButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Lesson")
                .setMessage("Are you sure you want to go back? Your progress might not be saved.")
                .setPositiveButton("Yes", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
