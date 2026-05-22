package com.example.roomieshare;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.content.Context;
import android.app.Activity;
import com.google.firebase.firestore.DocumentSnapshot;

public class ChoreListFragment extends Fragment {
    private RecyclerView recyclerViewYourTasks;
    private RecyclerView recyclerViewMembersTasks;
    private RecyclerView recyclerViewCompletedTasks;
    private ChoreListAdapter yourTasksAdapter;
    private ChoreListAdapter membersTasksAdapter;
    private ChoreListAdapter completedTasksAdapter;
    private List<Map<String, Object>> yourTasks;
    private List<Map<String, Object>> membersTasks;
    private List<Map<String, Object>> completedTasks;
    private android.widget.TextView completedCountText;
    private FirebaseFirestore db;
    private String currentUserUid;
    private String currentGroupId;
    private ListenerRegistration choresListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chore_list, container, false);
        
        // Initialize RecyclerViews
        recyclerViewYourTasks = view.findViewById(R.id.recyclerViewYourTasks);
        recyclerViewMembersTasks = view.findViewById(R.id.recyclerViewMembersTasks);
        recyclerViewCompletedTasks = view.findViewById(R.id.recyclerViewCompletedTasks);
        completedCountText = view.findViewById(R.id.completedCountText);
        
        // Initialize lists before adapters
        yourTasks = new ArrayList<>();
        membersTasks = new ArrayList<>();
        completedTasks = new ArrayList<>();
        
        // Set up layout managers with null check
        Context context = getContext();
        if (context != null) {
            recyclerViewYourTasks.setLayoutManager(new LinearLayoutManager(context));
            recyclerViewMembersTasks.setLayoutManager(new LinearLayoutManager(context));
            recyclerViewCompletedTasks.setLayoutManager(new LinearLayoutManager(context));
        }

        // Initialize adapters
        yourTasksAdapter = new ChoreListAdapter(requireContext(), yourTasks);
        membersTasksAdapter = new ChoreListAdapter(requireContext(), membersTasks);
        completedTasksAdapter = new ChoreListAdapter(requireContext(), completedTasks);
        
        // Set adapters
        if (recyclerViewYourTasks != null) recyclerViewYourTasks.setAdapter(yourTasksAdapter);
        if (recyclerViewMembersTasks != null) recyclerViewMembersTasks.setAdapter(membersTasksAdapter);
        if (recyclerViewCompletedTasks != null) recyclerViewCompletedTasks.setAdapter(completedTasksAdapter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserUid = currentUser.getUid();
            fetchCurrentGroupId();
        } else {
            Context ctx = getContext();
            if (ctx != null) {
                Toast.makeText(ctx, "Please sign in to continue", Toast.LENGTH_SHORT).show();
            }
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }

        FloatingActionButton fab = view.findViewById(R.id.fabAddChore);
        if (fab != null) {
            fab.setOnClickListener(v -> showAddChoreDialog());
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchCurrentGroupId();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (choresListener != null) {
            choresListener.remove();
            choresListener = null;
        }
    }

    private void fetchCurrentGroupId() {
        if (currentUserUid == null) return;
        db.collection("users").document(currentUserUid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    currentGroupId = documentSnapshot.getString("groupId");
                    if (currentGroupId != null) {
                        attachChoresListener();
                    }
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to fetch group information", Toast.LENGTH_SHORT).show();
            });
    }

    private void attachChoresListener() {
        if (choresListener != null) {
            choresListener.remove();
        }
        if (currentGroupId == null || db == null) return;

        choresListener = db.collection("chores")
            .whereEqualTo("groupId", currentGroupId)
            .addSnapshotListener((value, error) -> {
                Context context = getContext();
                if (context == null) return;

                if (error != null) {
                    Toast.makeText(context, "Error loading chores: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (yourTasks == null) yourTasks = new ArrayList<>();
                if (membersTasks == null) membersTasks = new ArrayList<>();
                if (completedTasks == null) completedTasks = new ArrayList<>();

                yourTasks.clear();
                membersTasks.clear();
                completedTasks.clear();

                if (value != null) {
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        if (doc != null && doc.exists()) {
                            String title = doc.getString("title");
                            String assignedTo = doc.getString("assignedTo");
                            String assigneeName = doc.getString("assigneeName");
                            Boolean done = doc.getBoolean("done");
                            String docId = doc.getId();

                            if (title != null && docId != null) {
                                Map<String, Object> chore = new HashMap<>();
                                chore.put("done", done != null ? done : false);
                                chore.put("id", docId);
                                chore.put("assignedTo", assignedTo);
                                boolean isDone = done != null && done;

                                // Always show in assignment-based section (checkbox shows done state)
                                if (assignedTo != null && assignedTo.equals(currentUserUid)) {
                                    chore.put("title", title);
                                    yourTasks.add(chore);
                                } else {
                                    String label = title;
                                    if (assigneeName != null && !assigneeName.isEmpty()) {
                                        label = title + " (" + assigneeName + ")";
                                    }
                                    chore.put("title", label);
                                    membersTasks.add(chore);
                                }

                                // Also add to completed summary if done
                                if (isDone) {
                                    Map<String, Object> completedChore = new HashMap<>(chore);
                                    String label = title;
                                    if (assignedTo != null && assignedTo.equals(currentUserUid)) {
                                        label = title + " (You)";
                                    } else if (assigneeName != null && !assigneeName.isEmpty()) {
                                        label = title + " (" + assigneeName + ")";
                                    }
                                    completedChore.put("title", label);
                                    completedTasks.add(completedChore);
                                }
                            }
                        }
                    }
                }

                if (yourTasksAdapter != null) yourTasksAdapter.updateData(yourTasks);
                if (membersTasksAdapter != null) membersTasksAdapter.updateData(membersTasks);
                if (completedTasksAdapter != null) completedTasksAdapter.updateData(completedTasks);
                if (completedCountText != null) {
                    completedCountText.setText(completedTasks.size() + " done");
                }
            });
    }

    private void showAddChoreDialog() {
        if (getContext() == null) return;
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_chore, null);
        EditText editTitle = dialogView.findViewById(R.id.editChoreTitle);
        EditText editDesc = dialogView.findViewById(R.id.editChoreDescription);
        Button dateButton = dialogView.findViewById(R.id.buttonPickDueDate);
        final Calendar dueDate = Calendar.getInstance();
        final boolean[] datePicked = {false};

        dateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    dueDate.set(year, month, dayOfMonth);
                    dateButton.setText(new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(dueDate.getTime()));
                    datePicked[0] = true;
                },
                dueDate.get(Calendar.YEAR),
                dueDate.get(Calendar.MONTH),
                dueDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        new AlertDialog.Builder(getContext())
            .setTitle("Add Chore")
            .setView(dialogView)
            .setPositiveButton("Add", (dialog, which) -> {
                String title = editTitle.getText().toString().trim();
                String desc = editDesc.getText().toString().trim();
                if (title.isEmpty()) {
                    Toast.makeText(getContext(), "Title required", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentGroupId == null) {
                    Toast.makeText(getContext(), "No group found", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Fetch group members for round-robin assignment
                db.collection("groups").document(currentGroupId).get()
                    .addOnSuccessListener(groupSnapshot -> {
                        if (!isAdded() || getContext() == null) return;
                        
                        if (groupSnapshot == null || !groupSnapshot.exists()) {
                            Toast.makeText(getContext(), "Group not found", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        @SuppressWarnings("unchecked")
                        List<String> members = (List<String>) groupSnapshot.get("members");
                        if (members == null || members.isEmpty()) {
                            Toast.makeText(getContext(), "No group members found", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Get persisted round-robin index from Firestore
                        Long storedIndex = groupSnapshot.getLong("lastAssignedIndex");
                        int lastIdx = (storedIndex != null) ? storedIndex.intValue() : -1;
                        int nextIndex = (lastIdx + 1) % members.size();
                        String assigneeUid = members.get(nextIndex);

                        // Persist the new index back to Firestore
                        db.collection("groups").document(currentGroupId)
                            .update("lastAssignedIndex", nextIndex);

                        // Fetch assignee's username
                        db.collection("users").document(assigneeUid).get()
                            .addOnSuccessListener(userSnapshot -> {
                                if (!isAdded()) return;
                                
                                String assigneeName = userSnapshot.getString("username");
                                if (assigneeName == null || assigneeName.isEmpty()) {
                                    assigneeName = userSnapshot.getString("displayName");
                                }
                                if (assigneeName == null || assigneeName.isEmpty()) {
                                    assigneeName = assigneeUid;
                                }
                                Map<String, Object> chore = new HashMap<>();
                                chore.put("title", title);
                                chore.put("description", desc);
                                chore.put("assignedTo", assigneeUid);
                                chore.put("assigneeName", assigneeName);
                                if (datePicked[0]) {
                                    chore.put("dueDate", dueDate.getTimeInMillis());
                                }
                                chore.put("groupId", currentGroupId);
                                db.collection("chores").add(chore)
                                    .addOnSuccessListener(documentReference -> Toast.makeText(getContext(), "Chore added!", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add chore", Toast.LENGTH_SHORT).show());
                            });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to fetch group information", Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
} 