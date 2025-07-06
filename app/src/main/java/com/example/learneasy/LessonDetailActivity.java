package com.example.learneasy;

import android.app.AlertDialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.IOException;
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

    private final Handler handler = new Handler();
    private MediaPlayer mediaPlayer; // For lesson-level audio
    private ImageButton currentPlayButton;

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
            releaseMediaPlayer();
            loadLesson(false);
        });
    }

    private void checkIfAttemptedThenLoadLesson() {
        userAnswers.clear();
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("quizAnswers")
                .child(level)
                .child(skill)
                .child(lessonId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
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
        FirebaseDatabase.getInstance()
                .getReference("content")
                .child(level.toLowerCase())
                .child(skill.toLowerCase())
                .child(lessonId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        lessonTitle.setText(snapshot.child("title").getValue(String.class));
                        lessonParagraph.setText(snapshot.child("paragraph").getValue(String.class));
                        quizContainer.removeAllViews();

                        String lessonAudioUrl = snapshot.child("audioUrl").getValue(String.class);
                        if (lessonAudioUrl != null && !lessonAudioUrl.trim().isEmpty()) {
                            View audioLayout = getLayoutInflater().inflate(R.layout.audio_player_layout, null);
                            ImageButton playButton = audioLayout.findViewById(R.id.playButton);
                            SeekBar seekBar = audioLayout.findViewById(R.id.seekBar);
                            TextView timeLabel = audioLayout.findViewById(R.id.timeLabel);

                            mediaPlayer = new MediaPlayer();
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            try {
                                mediaPlayer.setDataSource(lessonAudioUrl);
                                mediaPlayer.prepareAsync();
                            } catch (IOException e) {
                                Toast.makeText(LessonDetailActivity.this, "Invalid audio URL", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }

                            mediaPlayer.setOnPreparedListener(mp -> {
                                seekBar.setMax(mp.getDuration());
                                timeLabel.setText("00:00 / " + formatTime(mp.getDuration()));
                            });

                            playButton.setOnClickListener(v -> {
                                if (mediaPlayer.isPlaying()) {
                                    mediaPlayer.pause();
                                    playButton.setImageResource(R.drawable.play_circle);
                                } else {
                                    mediaPlayer.start();
                                    playButton.setImageResource(R.drawable.pause);
                                    updateSeekBar(seekBar, mediaPlayer, timeLabel, playButton);
                                }
                                currentPlayButton = playButton;
                            });

                            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                    if (fromUser) mediaPlayer.seekTo(progress);
                                }
                                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                            });

                            quizContainer.addView(audioLayout);
                        }

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
                                if (readOnlyMode && userAnswers.containsKey(quizId)) {
                                    rb.setEnabled(false);
                                    String selected = userAnswers.get(quizId);
                                    if (rb.getText().toString().equals(selected)) {
                                        rb.setChecked(true);
                                        rb.setTextColor(selected.equalsIgnoreCase(correct)
                                                ? ContextCompat.getColor(LessonDetailActivity.this, R.color.successGreen)
                                                : ContextCompat.getColor(LessonDetailActivity.this, R.color.errorRed));
                                    }
                                }
                                group.addView(rb);
                            }

                            quizContainer.addView(group);

                            Space space = new Space(LessonDetailActivity.this);
                            space.setMinimumHeight(30);
                            quizContainer.addView(space);

                            questionGroups.put(quizId, group);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(LessonDetailActivity.this, "Failed to load lesson.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateSeekBar(SeekBar seekBar, MediaPlayer player, TextView timeLabel, ImageButton playButton) {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (player != null && player.isPlaying()) {
                    seekBar.setProgress(player.getCurrentPosition());
                    timeLabel.setText(formatTime(player.getCurrentPosition()) + " / " + formatTime(player.getDuration()));
                    handler.postDelayed(this, 500);
                } else {
                    playButton.setImageResource(R.drawable.play_circle);
                }
            }
        }, 0);
    }

    private String formatTime(int milliseconds) {
        int mins = (milliseconds / 1000) / 60;
        int secs = (milliseconds / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
    }

    private boolean validateAllAnswered() {
        for (RadioGroup group : questionGroups.values()) {
            if (group.getCheckedRadioButtonId() == -1) return false;
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
            RadioButton selected = findViewById(group.getCheckedRadioButtonId());
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
                .child(level)
                .child(skill)
                .child(lessonId)
                .setValue(result);

        submitButton.setEnabled(false);
        submitButton.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            if (currentPlayButton != null) {
                currentPlayButton.setImageResource(R.drawable.play_circle);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseMediaPlayer();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Lesson")
                .setMessage("Are you sure you want to go back?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    releaseMediaPlayer();
                    super.onBackPressed();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
