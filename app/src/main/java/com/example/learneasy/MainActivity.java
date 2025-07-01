package com.example.learneasy;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;



public class MainActivity extends AppCompatActivity {

    private NavController navController;
    ImageView profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup NavController
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Setup BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // ✅ AppBarConfiguration with top-level destinations
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment,
                R.id.profileFragment
                // Don't add learnFragment here, as it's not a main tab
        ).build();

        // ✅ Hook BottomNavigationView with NavController
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Load circular logo
        ImageView imageView = findViewById(R.id.commonLogo);
        Glide.with(this)
                .load(R.drawable.learneasy_logo)
                .circleCrop()
                .into(imageView);

        // Profile icon click
        profileIcon = findViewById(R.id.profile_Icon);
        profileIcon.setOnClickListener(v -> showProfileDialog());

        // Optional: Update BottomNavigationView state on destination change
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.learnFragment) {
                bottomNavigationView.getMenu().setGroupCheckable(0, false, true); // disables selection
            } else {
                bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
            }
        });

//        profileIcon = findViewById(R.id.profile_Icon);
//
//        profileIcon.setOnClickListener(v -> {
//            // Load the ProfileFragment
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragment_container, new ProfileFragment())
//                    .addToBackStack(null)
//                    .commit();
//        });
    }

    private void showProfileDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) return;

        String uid = user.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.getValue(String.class);
                if (name == null || name.trim().isEmpty()) {
                    name = "User";
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Profile");
                builder.setMessage("Hi, " + name + " 👋\n\nDo you want to logout?");
                builder.setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                builder.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load name", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.child("imageIndex").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long imgIndex = snapshot.getValue(Long.class);
                if (imgIndex != null) {
                    int index = imgIndex.intValue();
                    int[] profilePics = {
                            R.drawable.cat1, R.drawable.cat2, R.drawable.cat3, R.drawable.cat4,
                            R.drawable.cat5, R.drawable.cat6, R.drawable.cat7, R.drawable.cat8
                    };

                    if (index >= 0 && index < profilePics.length) {
                        Glide.with(MainActivity.this)
                                .load(profilePics[index])
                                .circleCrop()
                                .into(profileIcon);  // update the top-right icon
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void loadProfileImage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.child("imageIndex").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long imgIndex = snapshot.getValue(Long.class);
                if (imgIndex != null) {
                    int index = imgIndex.intValue();
                    int[] profilePics = {
                            R.drawable.cat1, R.drawable.cat2, R.drawable.cat3, R.drawable.cat4,
                            R.drawable.cat5, R.drawable.cat6, R.drawable.cat7, R.drawable.cat8
                    };

                    if (index >= 0 && index < profilePics.length && profileIcon != null) {
                        Glide.with(MainActivity.this)
                                .load(profilePics[index])
                                .circleCrop()
                                .into(profileIcon);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error loading profile image", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
