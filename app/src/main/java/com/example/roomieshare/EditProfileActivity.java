package com.example.roomieshare;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private EditText editUsername, editEmail, editCurrentPassword, editNewPassword;
    private Button buttonSave;
    private FirebaseUser user;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        
        setContentView(R.layout.activity_edit_profile);

        android.view.View rootView = findViewById(android.R.id.content);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });

        editUsername        = findViewById(R.id.editProfileUsername);
        editEmail           = findViewById(R.id.editProfileEmail);
        editCurrentPassword = findViewById(R.id.editProfileCurrentPassword);
        editNewPassword     = findViewById(R.id.editProfilePassword);
        buttonSave          = findViewById(R.id.buttonSaveProfile);
        user                = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            editUsername.setText(user.getDisplayName());
            editEmail.setText(user.getEmail());
        }

        buttonSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String newUsername    = editUsername.getText().toString().trim();
        String newEmail       = editEmail.getText().toString().trim();
        String currentPassword = editCurrentPassword.getText().toString();
        String newPassword    = editNewPassword.getText().toString().trim();

        if (user == null) return;

        boolean wantsPasswordChange = !newPassword.isEmpty();
        boolean wantsEmailChange    = !newEmail.equals(user.getEmail()) && !newEmail.isEmpty();

        // If user wants to change password or email, current password is required
        if ((wantsPasswordChange || wantsEmailChange) && TextUtils.isEmpty(currentPassword)) {
            editCurrentPassword.setError("Current password is required to change email or password");
            editCurrentPassword.requestFocus();
            return;
        }

        buttonSave.setEnabled(false);

        // Step 1: Update display name (safe, no re-auth needed)
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                buttonSave.setEnabled(true);
                Toast.makeText(this, "Profile update failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            if (wantsPasswordChange || wantsEmailChange) {
                // Step 2: Re-authenticate first, then update sensitive fields
                reauthenticateThenUpdate(currentPassword, newEmail, newPassword, wantsEmailChange, wantsPasswordChange, newUsername);
            } else {
                // No sensitive changes — we're done
                syncProfileToFirestore(newUsername, null);
            }
        });
    }

    private void reauthenticateThenUpdate(String currentPassword, String newEmail,
                                          String newPassword, boolean changeEmail, boolean changePassword, String newUsername) {
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential).addOnCompleteListener(reAuthTask -> {
            if (!reAuthTask.isSuccessful()) {
                buttonSave.setEnabled(true);
                editCurrentPassword.setError("Incorrect current password");
                editCurrentPassword.requestFocus();
                Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_LONG).show();
                return;
            }

            // Re-auth succeeded — now update email first, then password
            if (changeEmail) {
                user.updateEmail(newEmail).addOnCompleteListener(emailTask -> {
                    if (!emailTask.isSuccessful()) {
                        buttonSave.setEnabled(true);
                        Toast.makeText(this, "Email update failed: " + emailTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (changePassword) {
                        updatePassword(newPassword, newUsername, newEmail);
                    } else {
                        syncProfileToFirestore(newUsername, newEmail);
                    }
                });
            } else if (changePassword) {
                updatePassword(newPassword, newUsername, null);
            }
        });
    }

    private void updatePassword(String newPassword, String newUsername, String newEmail) {
        user.updatePassword(newPassword).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                syncProfileToFirestore(newUsername, newEmail);
            } else {
                buttonSave.setEnabled(true);
                Toast.makeText(this, "Password update failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void syncProfileToFirestore(String username, @Nullable String email) {
        if (user == null) {
            buttonSave.setEnabled(true);
            finish();
            return;
        }
        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        if (email != null) {
            userData.put("email", email);
        }
        db.collection("users").document(uid)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnCompleteListener(task -> {
                buttonSave.setEnabled(true);
                if (task.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Firestore sync failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
    }
}