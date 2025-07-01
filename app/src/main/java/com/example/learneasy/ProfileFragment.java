package com.example.learneasy;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class ProfileFragment extends Fragment {

    TextView userName;
    EditText editName;
    Button editButton, saveButton, statusButton;
    ImageView profileImage, editIcon;
    LinearLayout mainProfile;

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
        editName = view.findViewById(R.id.edit_name);
        editButton = view.findViewById(R.id.edit_button);
        saveButton = view.findViewById(R.id.save_button);
        statusButton = view.findViewById(R.id.status_button);
        profileImage = view.findViewById(R.id.profile_image);
        editIcon = view.findViewById(R.id.edit_icon);
        mainProfile = view.findViewById(R.id.fragment_main_container); // Should match your XML ID

        // Get logged-in Firebase user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            loadUserProfile(); // 🔁 Load profile on start
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }

        // Edit name
        editButton.setOnClickListener(v -> {
            userName.setVisibility(View.GONE);
            editName.setVisibility(View.VISIBLE);
            saveButton.setVisibility(View.VISIBLE);
        });

        // Save name to Firebase
        saveButton.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            if (!newName.isEmpty()) {
                userName.setText(newName);
                dbRef.child("name").setValue(newName); // ✅ Save name
            }
            userName.setVisibility(View.VISIBLE);
            editName.setVisibility(View.GONE);
            saveButton.setVisibility(View.GONE);
        });

        // Status button (optional)
        statusButton.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Status button clicked", Toast.LENGTH_SHORT).show());

        // Image click listeners
        profileImage.setOnClickListener(v -> showImagePickerDialog());
        editIcon.setOnClickListener(v -> showPopupMenu(v));

        return view;
    }

    // 🔄 Load profile from Firebase
    private void loadUserProfile() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                Integer imageIndex = snapshot.child("imageIndex").getValue(Integer.class);

                if (name != null) {
                    userName.setText(name);
                    editName.setText(name);
                }

                if (imageIndex != null && imageIndex >= 0 && imageIndex < profilePics.length) {
                    selectedImageIndex = imageIndex;
                    profileImage.setImageResource(profilePics[selectedImageIndex]);
                    Log.d("FirebaseLoad", "Loaded imageIndex: " + imageIndex);
                } else {
                    profileImage.setImageResource(profilePics[0]); // fallback
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🖼️ Show profile image picker dialog
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
                dbRef.child("imageIndex").setValue(selectedImageIndex); // ✅ Save to Firebase
                Log.d("ProfileImage", "Saved image index: " + selectedImageIndex);
                dialog.dismiss();
            });
        }
    }

    // ☰ Edit menu popup
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
}
