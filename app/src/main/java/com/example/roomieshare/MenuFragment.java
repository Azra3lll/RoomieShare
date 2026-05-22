package com.example.roomieshare;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MenuFragment extends Fragment {
    private TextView avatarInitial;
    private TextView menuUserName;
    private TextView menuUserEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        // Populate profile info
        avatarInitial = view.findViewById(R.id.avatarInitial);
        menuUserName  = view.findViewById(R.id.menuUserName);
        menuUserEmail = view.findViewById(R.id.menuUserEmail);

        // Profile card → open ProfileActivity
        MaterialCardView profileCard = view.findViewById(R.id.profileCard);
        profileCard.setOnClickListener(v ->
            startActivity(new Intent(getActivity(), ProfileActivity.class))
        );

        // My Group
        MaterialCardView menuGroupCard = view.findViewById(R.id.menuGroupCard);
        menuGroupCard.setOnClickListener(v -> navigateTo(new GroupFragment()));

        // Chore Tracker
        MaterialCardView menuChoresCard = view.findViewById(R.id.menuChoresCard);
        menuChoresCard.setOnClickListener(v ->
            startActivity(new Intent(getActivity(), ChoreListHostActivity.class))
        );

        // Grocery List
        MaterialCardView menuGroceriesCard = view.findViewById(R.id.menuGroceriesCard);
        menuGroceriesCard.setOnClickListener(v -> navigateTo(new GroceryListFragment()));

        // Bills & Expenses
        MaterialCardView menuExpensesCard = view.findViewById(R.id.menuExpensesCard);
        menuExpensesCard.setOnClickListener(v -> navigateTo(new ExpenseFragment()));

        // Sign Out
        MaterialCardView menuLogoutCard = view.findViewById(R.id.menuLogoutCard);
        menuLogoutCard.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name  = user.getDisplayName();
            String email = user.getEmail();
            String displayName = (name != null && !name.isEmpty()) ? name : "Roomie";
            menuUserName.setText(displayName);
            menuUserEmail.setText(email != null ? email : "");
            if (!displayName.isEmpty()) {
                avatarInitial.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
            }
        }
    }

    private void navigateTo(Fragment fragment) {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
        }
    }

}