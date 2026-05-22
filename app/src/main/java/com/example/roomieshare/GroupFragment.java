package com.example.roomieshare;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.android.material.button.MaterialButton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ProgressDialog progressDialog;
    private LinearLayout groupOptionsLayout;
    private View groupInfoCard;
    private TextView groupNameText;
    private TextView groupCodeText;
    private LinearLayout groupMembersLayout;
    private Button leaveGroupButton;
    private MaterialButton editGroupButton;

    // Track current group state
    private String currentGroupId;
    private String currentCreatedBy;
    private boolean isCreator = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        groupOptionsLayout  = view.findViewById(R.id.groupOptionsLayout);
        groupInfoCard       = view.findViewById(R.id.groupInfoCard);
        groupNameText       = view.findViewById(R.id.textGroupName);
        groupCodeText       = view.findViewById(R.id.textGroupCode);
        groupMembersLayout  = view.findViewById(R.id.layoutGroupMembers);
        leaveGroupButton    = view.findViewById(R.id.buttonLeaveGroup);
        editGroupButton     = view.findViewById(R.id.buttonEditGroup);

        Button createGroupButton = view.findViewById(R.id.buttonCreateGroup);
        Button joinGroupButton   = view.findViewById(R.id.buttonJoinGroup);

        createGroupButton.setOnClickListener(v -> showCreateGroupDialog());
        joinGroupButton.setOnClickListener(v -> showJoinGroupDialog());
        leaveGroupButton.setOnClickListener(v -> showLeaveGroupDialog());
        editGroupButton.setOnClickListener(v -> showEditGroupDialog());

        checkGroupStatus();

        return view;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Group status
    // ─────────────────────────────────────────────────────────────────

    private void checkGroupStatus() {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                String groupId = documentSnapshot.getString("groupId");
                if (!TextUtils.isEmpty(groupId)) {
                    currentGroupId = groupId;
                    showGroupInfo(groupId);
                } else {
                    currentGroupId = null;
                    showGroupOptions();
                }
            })
            .addOnFailureListener(e -> showGroupOptions());
    }

    private void showGroupOptions() {
        groupOptionsLayout.setVisibility(View.VISIBLE);
        groupInfoCard.setVisibility(View.GONE);
    }

    private void showGroupInfo(String groupId) {
        db.collection("groups").document(groupId).get()
            .addOnSuccessListener(groupSnapshot -> {
                if (groupSnapshot == null || !groupSnapshot.exists()) return;

                String groupName = groupSnapshot.getString("name");
                String groupCode = groupSnapshot.getString("code");
                currentCreatedBy = groupSnapshot.getString("createdBy");

                groupNameText.setText(groupName != null ? groupName : "");
                groupCodeText.setText("Code: " + (groupCode != null ? groupCode : ""));

                groupOptionsLayout.setVisibility(View.GONE);
                groupInfoCard.setVisibility(View.VISIBLE);

                isCreator = currentUser != null && currentUser.getUid().equals(currentCreatedBy);
                editGroupButton.setVisibility(isCreator ? View.VISIBLE : View.GONE);

                @SuppressWarnings("unchecked")
                List<String> members = (List<String>) groupSnapshot.get("members");
                renderMembers(members, groupId, isCreator);
            })
            .addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to load group information", Toast.LENGTH_SHORT).show());
    }

    // ─────────────────────────────────────────────────────────────────
    //  Members list
    // ─────────────────────────────────────────────────────────────────

    private void renderMembers(List<String> members, String groupId, boolean showRemoveButtons) {
        groupMembersLayout.removeAllViews();

        int colorOnSurface = android.graphics.Color.BLACK;
        if (getContext() != null) {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            if (getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)) {
                colorOnSurface = typedValue.data;
            }
        }
        final int finalColorOnSurface = colorOnSurface;

        if (members == null || members.isEmpty()) {
            if (!isAdded()) return;
            TextView noMembers = new TextView(requireContext());
            noMembers.setText("No members");
            noMembers.setTextColor(colorOnSurface);
            groupMembersLayout.addView(noMembers);
            return;
        }

        for (String memberId : members) {
            db.collection("users").document(memberId).get()
                .addOnSuccessListener(userSnapshot -> {
                    if (!isAdded() || getContext() == null) return;

                    String username = userSnapshot.getString("username");
                    if (username == null || username.isEmpty()) username = userSnapshot.getString("displayName");
                    if (username == null || username.isEmpty()) username = memberId;

                    // Row: avatar emoji + name + (optional) Remove button
                    LinearLayout row = new LinearLayout(getContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    rowParams.setMargins(0, 0, 0, 12);
                    row.setLayoutParams(rowParams);
                    row.setPadding(12, 10, 12, 10);
                    row.setBackgroundResource(R.drawable.item_background_rounded);

                    // Avatar label
                    TextView avatar = new TextView(getContext());
                    avatar.setText("👤");
                    avatar.setTextSize(20f);
                    LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    avatarParams.setMarginEnd(12);
                    avatar.setLayoutParams(avatarParams);
                    row.addView(avatar);

                    // Name label (takes remaining space)
                    TextView nameLabel = new TextView(getContext());
                    String displayName = username;
                    boolean isSelf    = memberId.equals(currentUser != null ? currentUser.getUid() : "");
                    boolean isOwner   = currentCreatedBy != null && memberId.equals(currentCreatedBy);
                    String badge      = isSelf ? " (You)" : "";
                    String ownerBadge = isOwner ? " 👑" : "";
                    nameLabel.setText(displayName + ownerBadge + badge);
                    nameLabel.setTextSize(15f);
                    nameLabel.setTextColor(finalColorOnSurface);
                    LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    nameLabel.setLayoutParams(nameParams);
                    row.addView(nameLabel);

                    // Remove button — only for creator, and not for themselves
                    if (showRemoveButtons && !isSelf) {
                        MaterialButton removeBtn = new MaterialButton(getContext());
                        removeBtn.setText("Remove");
                        removeBtn.setTextSize(12f);
                        removeBtn.setTextColor(android.graphics.Color.parseColor("#E53935"));
                        removeBtn.setAllCaps(false);
                        removeBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                        removeBtn.setStrokeColor(android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#E53935")));
                        removeBtn.setStrokeWidth(2);
                        removeBtn.setCornerRadius(24);
                        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        removeBtn.setLayoutParams(btnParams);
                        removeBtn.setOnClickListener(v ->
                                showRemoveMemberDialog(groupId, memberId, displayName));
                        row.addView(removeBtn);
                    }

                    groupMembersLayout.addView(row);
                });
        }
    }



    // ─────────────────────────────────────────────────────────────────
    //  Remove member
    // ─────────────────────────────────────────────────────────────────

    private void showRemoveMemberDialog(String groupId, String memberId, String memberName) {
        new AlertDialog.Builder(getContext())
            .setTitle("Remove Member")
            .setMessage("Remove \"" + memberName + "\" from the group?")
            .setPositiveButton("Remove", (dialog, which) -> removeMember(groupId, memberId))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void removeMember(String groupId, String memberId) {
        if (getContext() == null) return;
        progressDialog = ProgressDialog.show(getContext(), "Removing Member", "Please wait...", true);

        // Remove from group's members array
        db.collection("groups").document(groupId)
            .update("members", FieldValue.arrayRemove(memberId))
            .addOnSuccessListener(aVoid -> {
                // Clear the member's groupId in their user document
                Map<String, Object> userUpdate = new HashMap<>();
                userUpdate.put("groupId", null);
                userUpdate.put("groupRole", null);
                db.collection("users").document(memberId)
                    .update(userUpdate)
                    .addOnCompleteListener(task -> {
                        if (progressDialog != null) progressDialog.dismiss();
                        Toast.makeText(getContext(), "Member removed", Toast.LENGTH_SHORT).show();
                        // Refresh
                        showGroupInfo(groupId);
                    });
            })
            .addOnFailureListener(e -> {
                if (progressDialog != null) progressDialog.dismiss();
                Toast.makeText(getContext(), "Failed to remove member: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Edit group
    // ─────────────────────────────────────────────────────────────────

    private void showEditGroupDialog() {
        if (!isAdded() || getContext() == null) return;

        EditText editGroupName = new EditText(getContext());
        editGroupName.setHint("New group name");
        editGroupName.setText(groupNameText.getText().toString());
        editGroupName.setSelection(editGroupName.getText().length());
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        editGroupName.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(getContext())
            .setTitle("Edit Group")
            .setView(editGroupName)
            .setPositiveButton("Save", (dialog, which) -> {
                String newName = editGroupName.getText().toString().trim();
                if (TextUtils.isEmpty(newName)) {
                    Toast.makeText(getContext(), "Group name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveGroupName(currentGroupId, newName);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void saveGroupName(String groupId, String newName) {
        if (groupId == null) return;
        progressDialog = ProgressDialog.show(getContext(), "Saving", "Please wait...", true);
        db.collection("groups").document(groupId)
            .update("name", newName)
            .addOnSuccessListener(aVoid -> {
                if (progressDialog != null) progressDialog.dismiss();
                groupNameText.setText(newName);
                Toast.makeText(getContext(), "Group name updated!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                if (progressDialog != null) progressDialog.dismiss();
                Toast.makeText(getContext(), "Failed to update group name", Toast.LENGTH_SHORT).show();
            });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Create group
    // ─────────────────────────────────────────────────────────────────

    private void showCreateGroupDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_group, null);
        EditText editGroupName = dialogView.findViewById(R.id.editGroupName);

        new AlertDialog.Builder(getContext())
            .setTitle("Create Group")
            .setView(dialogView)
            .setPositiveButton("Create", (dialog, which) -> {
                String groupName = editGroupName.getText().toString().trim();
                if (TextUtils.isEmpty(groupName)) {
                    Toast.makeText(getContext(), "Group name required", Toast.LENGTH_SHORT).show();
                    return;
                }
                createGroupInFirestore(groupName);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void createGroupInFirestore(String groupName) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getContext() == null) return;
        progressDialog = ProgressDialog.show(getContext(), "Creating Group", "Please wait...", true);
        String groupCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String userId    = currentUser.getUid();

        Map<String, Object> group = new HashMap<>();
        group.put("name",      groupName);
        group.put("code",      groupCode);
        group.put("createdBy", userId);
        group.put("members",   java.util.Collections.singletonList(userId));

        db.collection("groups").add(group)
            .addOnSuccessListener(documentReference -> {
                currentGroupId = documentReference.getId();
                Map<String, Object> userUpdate = new HashMap<>();
                userUpdate.put("groupId",   currentGroupId);
                userUpdate.put("groupRole", "admin");
                db.collection("users").document(userId)
                    .set(userUpdate, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        if (progressDialog != null) progressDialog.dismiss();
                        Toast.makeText(getContext(), "Group created! Code: " + groupCode, Toast.LENGTH_LONG).show();
                        checkGroupStatus();
                    })
                    .addOnFailureListener(e -> {
                        if (progressDialog != null) progressDialog.dismiss();
                        Log.e("GroupFragment", "Failed to update user: ", e);
                        Toast.makeText(getContext(), "Group created, but failed to update user", Toast.LENGTH_LONG).show();
                    });
            })
            .addOnFailureListener(e -> {
                if (progressDialog != null) progressDialog.dismiss();
                Log.e("GroupFragment", "Failed to create group: ", e);
                Toast.makeText(getContext(), "Failed to create group: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Join group
    // ─────────────────────────────────────────────────────────────────

    private void showJoinGroupDialog() {
        EditText editGroupCode = new EditText(getContext());
        editGroupCode.setHint("Enter Group Code");
        editGroupCode.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        editGroupCode.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(getContext())
            .setTitle("Join Group")
            .setView(editGroupCode)
            .setPositiveButton("Join", (dialog, which) -> {
                String groupCode = editGroupCode.getText().toString().trim().toUpperCase();
                if (TextUtils.isEmpty(groupCode)) {
                    Toast.makeText(getContext(), "Group code required", Toast.LENGTH_SHORT).show();
                    return;
                }
                joinGroupWithCode(groupCode);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void joinGroupWithCode(String groupCode) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getContext() == null) return;
        progressDialog = ProgressDialog.show(getContext(), "Joining Group", "Please wait...", true);
        db.collection("groups").whereEqualTo("code", groupCode).get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.isEmpty()) {
                    if (progressDialog != null) progressDialog.dismiss();
                    Toast.makeText(getContext(), "Group not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                String groupId = querySnapshot.getDocuments().get(0).getId();
                db.collection("groups").document(groupId)
                    .update("members", FieldValue.arrayUnion(currentUser.getUid()))
                    .addOnSuccessListener(aVoid -> {
                        Map<String, Object> userUpdate = new HashMap<>();
                        userUpdate.put("groupId",   groupId);
                        userUpdate.put("groupRole", "member");
                        db.collection("users").document(currentUser.getUid())
                            .set(userUpdate, SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> {
                                if (progressDialog != null) progressDialog.dismiss();
                                currentGroupId = groupId;
                                Toast.makeText(getContext(), "Joined group!", Toast.LENGTH_SHORT).show();
                                checkGroupStatus();
                            })
                            .addOnFailureListener(e -> {
                                if (progressDialog != null) progressDialog.dismiss();
                                Toast.makeText(getContext(), "Failed to update user", Toast.LENGTH_SHORT).show();
                            });
                    })
                    .addOnFailureListener(e -> {
                        if (progressDialog != null) progressDialog.dismiss();
                        Toast.makeText(getContext(), "Failed to join group: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            })
            .addOnFailureListener(e -> {
                if (progressDialog != null) progressDialog.dismiss();
                Toast.makeText(getContext(), "Error finding group", Toast.LENGTH_SHORT).show();
            });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Leave group
    // ─────────────────────────────────────────────────────────────────

    private void showLeaveGroupDialog() {
        String message = isCreator
                ? "You are the group creator. If you leave, the group will remain but you will no longer be in it. Are you sure?"
                : "Are you sure you want to leave this group?";
        new AlertDialog.Builder(getContext())
            .setTitle("Leave Group")
            .setMessage(message)
            .setPositiveButton("Leave", (dialog, which) -> leaveGroup())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void leaveGroup() {
        if (currentUser == null) return;
        progressDialog = ProgressDialog.show(getContext(), "Leaving Group", "Please wait...", true);
        String userId = currentUser.getUid();

        // Remove from group members array first
        if (currentGroupId != null) {
            db.collection("groups").document(currentGroupId)
                .update("members", FieldValue.arrayRemove(userId))
                .addOnCompleteListener(t -> clearUserGroupData(userId));
        } else {
            clearUserGroupData(userId);
        }
    }

    private void clearUserGroupData(String userId) {
        Map<String, Object> update = new HashMap<>();
        update.put("groupId",   null);
        update.put("groupRole", null);
        db.collection("users").document(userId)
            .update(update)
            .addOnSuccessListener(aVoid -> {
                if (progressDialog != null) progressDialog.dismiss();
                currentGroupId = null;
                isCreator      = false;
                Toast.makeText(getContext(), "You left the group", Toast.LENGTH_SHORT).show();
                checkGroupStatus();
            })
            .addOnFailureListener(e -> {
                if (progressDialog != null) progressDialog.dismiss();
                Toast.makeText(getContext(), "Failed to leave group", Toast.LENGTH_SHORT).show();
            });
    }

}