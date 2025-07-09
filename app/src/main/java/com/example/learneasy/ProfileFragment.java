package com.example.learneasy;

import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProfileFragment extends Fragment {

    TextView userName, emailText, todayWordText;
    EditText editName;
    ImageView profileImage, editIcon, editNameIcon, doneIcon;
    LinearLayout nameDisplayRow, nameEditRow, mainProfile, todayWordContainer;

    int selectedImageIndex = 0;
    int[] profilePics = {
            R.drawable.cat1, R.drawable.cat2, R.drawable.cat3, R.drawable.cat4,
            R.drawable.cat5, R.drawable.cat6, R.drawable.cat7, R.drawable.cat8
    };

    DatabaseReference dbRef;
    String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Bind views
        userName = view.findViewById(R.id.user_name);
        emailText = view.findViewById(R.id.email_text);
        editName = view.findViewById(R.id.edit_name);
        profileImage = view.findViewById(R.id.profile_image);
        editIcon = view.findViewById(R.id.edit_icon);
        todayWordText = view.findViewById(R.id.today_word_text);

        editNameIcon = view.findViewById(R.id.edit_name_icon);
        doneIcon = view.findViewById(R.id.edit_done_icon);
        nameDisplayRow = view.findViewById(R.id.name_display_row);
        nameEditRow = view.findViewById(R.id.name_edit_row);
        mainProfile = view.findViewById(R.id.fragment_main_container);
        todayWordContainer = view.findViewById(R.id.today_word_container);

        // Firebase user check
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            // Set name and email from Firebase Auth
            userName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Your Name");
            editName.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
            emailText.setText(user.getEmail() != null ? user.getEmail() : "email@example.com");

            loadUserProfile();
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }

        // Name edit click
        editNameIcon.setOnClickListener(v -> {
            nameDisplayRow.setVisibility(View.GONE);
            nameEditRow.setVisibility(View.VISIBLE);
        });

        // Save (done) click
        doneIcon.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            if (!newName.isEmpty()) {
                userName.setText(newName);

                // Update Firebase Authentication displayName
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .build();
                    currentUser.updateProfile(profileUpdates)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("FirebaseAuth", "User display name updated.");
                                }
                            });
                }

                // Update name in Realtime Database
                dbRef.child("name").setValue(newName);
            }
            nameEditRow.setVisibility(View.GONE);
            nameDisplayRow.setVisibility(View.VISIBLE);
        });

        // Profile image handling
        profileImage.setOnClickListener(v -> showImagePickerDialog());
        editIcon.setOnClickListener(v -> showPopupMenu(v));

        fetchWordFromDictionaryApi();
        return view;
    }

    private void loadUserProfile() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer imageIndex = snapshot.child("imageIndex").getValue(Integer.class);
                if (imageIndex != null && imageIndex >= 0 && imageIndex < profilePics.length) {
                    selectedImageIndex = imageIndex;
                    profileImage.setImageResource(profilePics[selectedImageIndex]);
                } else {
                    profileImage.setImageResource(profilePics[0]);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.profile_image_picker_dialog, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        int[] viewIds = {
                R.id.img1, R.id.img2, R.id.img3, R.id.img4,
                R.id.img5, R.id.img6, R.id.img7, R.id.img8
        };

        for (int i = 0; i < viewIds.length; i++) {
            ImageView img = dialogView.findViewById(viewIds[i]);
            int index = i;
            img.setOnClickListener(v -> {
                selectedImageIndex = index;
                profileImage.setImageResource(profilePics[selectedImageIndex]);

                dbRef.child("imageIndex").setValue(selectedImageIndex)
                        .addOnSuccessListener(unused -> {
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).loadProfileImage();
                            }
                        });

                dialog.dismiss();
            });
        }
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(requireContext(), view);
        popup.getMenuInflater().inflate(R.menu.profile_popup_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_view_profile) {
                mainProfile.setVisibility(View.VISIBLE);
                return true;
            } else if (item.getItemId() == R.id.menu_change_profile) {
                showImagePickerDialog();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void fetchWordFromDictionaryApi() {
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        String[] sampleWords = {"serendipity", "eloquent", "gregarious", "quintessential", "ephemeral"};
        int index = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR) % sampleWords.length;
        String word = sampleWords[index];
        String url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + word;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject wordData = response.getJSONObject(0);
                        JSONArray meanings = wordData.getJSONArray("meanings");
                        JSONObject firstMeaning = meanings.getJSONObject(0);
                        String partOfSpeech = firstMeaning.optString("partOfSpeech", "");
                        JSONArray definitions = firstMeaning.getJSONArray("definitions");
                        String definition = definitions.getJSONObject(0).optString("definition", "No definition");
                        String example = definitions.getJSONObject(0).optString("example", "");

                        JSONArray phonetics = wordData.optJSONArray("phonetics");
                        String audioUrl = "";
                        String phoneticText = "";
                        if (phonetics != null && phonetics.length() > 0) {
                            for (int i = 0; i < phonetics.length(); i++) {
                                JSONObject phonetic = phonetics.getJSONObject(i);
                                if (phonetic.has("audio") && !phonetic.getString("audio").isEmpty()) {
                                    audioUrl = phonetic.getString("audio");
                                    phoneticText = phonetic.optString("text", "");
                                    break;
                                }
                            }
                        }

                        todayWordText.setText(word);
                        String finalExample = example;
                        String finalAudioUrl = audioUrl;
                        String finalPhoneticText = phoneticText;

                        todayWordContainer.setOnClickListener(v -> {
                            showFlashcardDialog(word, definition, finalExample, partOfSpeech, finalAudioUrl, finalPhoneticText);
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("WordFetch", "API error: " + error.getMessage()));

        queue.add(request);
    }

    private void showFlashcardDialog(String word, String definition, String example, String partOfSpeech, String audioUrl, String phoneticText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.flashcard_dialog, null);
        builder.setView(dialogView);

        TextView wordView = dialogView.findViewById(R.id.word_title);
        TextView defView = dialogView.findViewById(R.id.word_definition);
        TextView exampleView = dialogView.findViewById(R.id.word_example);
        TextView usageView = dialogView.findViewById(R.id.word_usage);
        TextView phoneticView = dialogView.findViewById(R.id.word_phonetic);
        ImageView closeIcon = dialogView.findViewById(R.id.close_button);
        ImageView audioIcon = dialogView.findViewById(R.id.audio_button);

        wordView.setText(word);
        defView.setText("Meaning: " + definition);
        usageView.setText("Part of Speech: " + partOfSpeech);
        phoneticView.setText("Phonetic: " + phoneticText);

        if (example != null && !example.isEmpty()) {
            exampleView.setVisibility(View.VISIBLE);
            exampleView.setText("Example: " + example);
        } else {
            exampleView.setVisibility(View.GONE);
        }

        audioIcon.setOnClickListener(v -> {
            if (!audioUrl.isEmpty()) {
                MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(audioUrl);
                    mediaPlayer.setOnPreparedListener(MediaPlayer::start);
                    mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                    mediaPlayer.prepareAsync();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Couldn't play audio", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(getContext(), "No audio available", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = builder.create();
        closeIcon.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
