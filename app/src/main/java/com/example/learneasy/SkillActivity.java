package com.example.learneasy;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SkillActivity extends AppCompatActivity {

    private TextView title, description;
    private String level, skill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skill);

        // Bind views
        title = findViewById(R.id.skillTitle);
        description = findViewById(R.id.skillDescription);
        ImageButton backButton = findViewById(R.id.backButton); // ✅ back button

        // Back button action to finish activity
        backButton.setOnClickListener(v -> finish());

        // Get intent extras with safety
        level = getIntent().getStringExtra("level");
        skill = getIntent().getStringExtra("skill");

        if (level != null && skill != null) {
            setDynamicContent(level, skill);
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
