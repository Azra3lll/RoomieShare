package com.example.roomieshare;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {
    private FirebaseFirestore db;
    private String currentUserUid;
    private int totalChores = 0;
    private int doneChores = 0;
    private FirebaseUser currentUser;
    private CalendarView calendarView;
    private com.google.firebase.firestore.ListenerRegistration choresListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        TextView welcomeText = view.findViewById(R.id.welcomeText);
        if (currentUser != null && currentUser.getDisplayName() != null) {
            welcomeText.setText("Welcome, " + currentUser.getDisplayName());
        }

        // Profile icon click
        ImageView profilePlaceholder = view.findViewById(R.id.profilePlaceholder);
        profilePlaceholder.setOnClickListener(v ->
            startActivity(new Intent(getActivity(), ProfileActivity.class))
        );

        // Set current date
        TextView currentDateText = view.findViewById(R.id.currentDateText);
        String currentDate = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(new Date());
        currentDateText.setText(currentDate);
        
        // Grab Calendar View
        calendarView = view.findViewById(R.id.calendarView);

        // Chore Tracker card click - Restore original functionality
        MaterialCardView cardChoreTracker = view.findViewById(R.id.cardChoreTracker);
        cardChoreTracker.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChoreListHostActivity.class);
            startActivity(intent);
        });

        // Bills card click
        MaterialCardView billsCard = view.findViewById(R.id.billsCard);
        billsCard.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                .replace(R.id.mainFragmentContainer, new ExpenseFragment())
                .addToBackStack(null)
                .commit();
        });

        // Groceries card click
        MaterialCardView groceriesCard = view.findViewById(R.id.groceriesCard);
        groceriesCard.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                .replace(R.id.mainFragmentContainer, new GroceryListFragment())
                .addToBackStack(null)
                .commit();
        });

        // Progress bar and text
        ProgressBar choreProgressBar = view.findViewById(R.id.choreProgressBar);
        TextView choreProgressText = view.findViewById(R.id.choreProgressText);

        // Firestore setup
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserUid = currentUser.getUid();
            listenForChoreProgress(choreProgressBar, choreProgressText);
        } else {
            // Session expired or invalidated — redirect to login
            startActivity(new Intent(getActivity(), LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }

        return view;
    }

    private void listenForChoreProgress(ProgressBar progressBar, TextView progressText) {
        if (choresListener != null) {
            choresListener.remove();
        }
        if (currentUserUid == null || db == null) return;

        choresListener = db.collection("chores")
            .whereEqualTo("assignedTo", currentUserUid)
            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@javax.annotation.Nullable QuerySnapshot value, @javax.annotation.Nullable FirebaseFirestoreException error) {
                    if (error != null) return;
                    totalChores = 0;
                    doneChores = 0;
                    
                    List<EventDay> events = new ArrayList<>();
                    
                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Boolean done = doc.getBoolean("done");
                            Long dueDateMs = doc.getLong("dueDate");
                            
                            totalChores++;
                            if (done != null && done) {
                                doneChores++;
                            } else if (dueDateMs != null) {
                                // Add a dot to calendar for pending chores using a calendar instance
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTimeInMillis(dueDateMs);
                                events.add(new EventDay(calendar, R.drawable.event_dot));
                            }
                        }
                    }
                    progressText.setText(doneChores + " of " + totalChores + " done");
                    int progress = (totalChores == 0) ? 0 : (int) ((doneChores * 100.0f) / totalChores);
                    progressBar.setProgress(progress);
                    
                    if (calendarView != null) {
                        calendarView.setEvents(events);
                    }
                }
            });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (choresListener != null) {
            choresListener.remove();
            choresListener = null;
        }
    }

}