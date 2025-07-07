package com.example.learneasy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SkillActivity extends AppCompatActivity {

    private TextView title, description, lessonCountText;
    private LinearLayout lessonListContainer;
    private String level, skill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill);

        // Bind views
        title = findViewById(R.id.skillTitle);
        description = findViewById(R.id.skillDescription);
        lessonCountText = findViewById(R.id.lessonCount);
        lessonListContainer = findViewById(R.id.lessonListContainer);
        ImageButton backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        // Get intent data
        level = getIntent().getStringExtra("level");
        skill = getIntent().getStringExtra("skill");

        if (level != null && skill != null) {
            setDynamicContent(level, skill);
            loadLessonsFromFirebase(level, skill);
        } else {
            title.setText("Skill Details");
            description.setText("No data available. Please try again.");
        }
    }

    private void setDynamicContent(String level, String skill) {
        String titleText = capitalize(level) + " Level: " + capitalize(skill);
        title.setText(titleText);

        String desc;
        switch (skill.toLowerCase()) {
            case "reading":
                desc = "Read, practice & test your reading skills.";
                break;
            case "listening":
                desc = "Listen carefully and improve your comprehension.";
                break;
            case "speaking":
                desc = "Speak confidently and get evaluated.";
                break;
            case "writing":
                desc = "Write and practice structured responses.";
                break;
            default:
                desc = "Skill description not found.";
                break;
        }
        description.setText(desc);
    }

    private void loadLessonsFromFirebase(String level, String skill) {
        DatabaseReference lessonsRef = FirebaseDatabase.getInstance()
                .getReference("content")
                .child(level.toLowerCase())
                .child(skill.toLowerCase());

        lessonsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int count = 0;
                lessonListContainer.removeAllViews();

                for (DataSnapshot lessonSnap : snapshot.getChildren()) {
                    count++;

                    String lessonKey = lessonSnap.getKey();
                    String lessonTitle = lessonSnap.child("title").getValue(String.class);

                    // Card container
                    LinearLayout cardLayout = new LinearLayout(SkillActivity.this);
                    cardLayout.setOrientation(LinearLayout.HORIZONTAL);
                    cardLayout.setBackgroundResource(R.drawable.rounded_card_background);
                    cardLayout.setPadding(24, 24, 24, 24);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 12, 0, 0);
                    cardLayout.setLayoutParams(params);

                    // ▶️ Lesson text
                    TextView lessonText = new TextView(SkillActivity.this);
                    lessonText.setText("▶️  Lesson #" + count);
                    lessonText.setTextSize(16);
                    lessonText.setTextColor(getColor(R.color.textPrimary));
                    lessonText.setLayoutParams(new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
                    ));

                    // ❓ Quiz text
                    TextView quizText = new TextView(SkillActivity.this);
                    quizText.setText("Quiz ❓");
                    quizText.setTextSize(16);
                    quizText.setTextColor(getColor(R.color.textPrimary));

                    cardLayout.addView(lessonText);
                    cardLayout.addView(quizText);

                    // Click to open detail
                    cardLayout.setOnClickListener(v -> {
                        Intent intent = new Intent(SkillActivity.this, LessonDetailActivity.class);
                        intent.putExtra("level", level);
                        intent.putExtra("skill", skill);
                        intent.putExtra("lessonId", lessonKey);
                        startActivity(intent);
                    });

                    lessonListContainer.addView(cardLayout);
                }

                lessonCountText.setText(count + " Lessons");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(SkillActivity.this, "Error loading lessons", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
