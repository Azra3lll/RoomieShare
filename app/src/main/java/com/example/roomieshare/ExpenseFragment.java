package com.example.roomieshare;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.Map;
import android.widget.ImageButton;

public class ExpenseFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LinearLayout billsLayout;
    private LinearLayout groceryLayout;
    private LinearLayout othersLayout;
    private TextView totalAmountText;
    private String groupId;
    private double totalAmount = 0.0;
    private int memberCount = 1;
    private TextView perPersonText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        billsLayout = view.findViewById(R.id.billsLayout);
        groceryLayout = view.findViewById(R.id.groceryLayout);
        othersLayout = view.findViewById(R.id.othersLayout);
        totalAmountText = view.findViewById(R.id.totalAmountText);
        perPersonText = view.findViewById(R.id.perPersonText);

        // Set up edit buttons
        ImageButton editBillsButton = view.findViewById(R.id.editBillsButton);
        ImageButton editGroceryButton = view.findViewById(R.id.editGroceryButton);
        ImageButton editOthersButton = view.findViewById(R.id.editOthersButton);

        editBillsButton.setOnClickListener(v -> toggleDeleteButtons(billsLayout, editBillsButton));
        editGroceryButton.setOnClickListener(v -> toggleDeleteButtons(groceryLayout, editGroceryButton));
        editOthersButton.setOnClickListener(v -> toggleDeleteButtons(othersLayout, editOthersButton));

        FloatingActionButton addExpenseButton = view.findViewById(R.id.fabAddExpense);
        addExpenseButton.setOnClickListener(v -> showAddExpenseDialog());

        loadGroupAndExpenses();

        return view;
    }

    private void toggleDeleteButtons(LinearLayout layout, ImageButton editButton) {
        boolean isEditing = editButton.getTag() != null && (boolean) editButton.getTag();
        
        // Toggle edit state
        isEditing = !isEditing;
        editButton.setTag(isEditing);
        
        // Update edit button appearance
        editButton.setImageResource(isEditing ? 
            android.R.drawable.ic_menu_save : 
            android.R.drawable.ic_menu_edit);

        // Show/hide delete buttons in all expense items
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof LinearLayout) {
                View deleteButton = ((LinearLayout) child).getChildAt(1);
                if (deleteButton != null) {
                    deleteButton.setVisibility(isEditing ? View.VISIBLE : View.GONE);
                }
            }
        }
    }

    private void loadGroupAndExpenses() {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!isAdded() || getContext() == null) return;
                groupId = documentSnapshot.getString("groupId");
                if (groupId != null) {
                    // Fetch member count for per-person split
                    db.collection("groups").document(groupId).get()
                        .addOnSuccessListener(groupSnapshot -> {
                            if (!isAdded()) return;
                            java.util.List<?> members = (java.util.List<?>) groupSnapshot.get("members");
                            if (members != null && !members.isEmpty()) {
                                memberCount = members.size();
                            } else {
                                memberCount = 1;
                            }
                            loadExpenses();
                        })
                        .addOnFailureListener(e -> loadExpenses());
                } else {
                    showNoGroupMessage();
                }
            });
    }

    private void loadExpenses() {
        billsLayout.removeAllViews();
        groceryLayout.removeAllViews();
        othersLayout.removeAllViews();
        totalAmount = 0.0;

        db.collection("expenses")
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!isAdded() || getContext() == null) return;
                if (querySnapshot != null) {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String category = document.getString("category");
                        if (category != null) {
                            addExpenseToCategory(document, category);
                        }
                    }
                    updateTotalAmount();
                }
            })
            .addOnFailureListener(e -> {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Failed to load expenses", Toast.LENGTH_SHORT).show();
            });
    }

    private void addExpenseToCategory(QueryDocumentSnapshot document, String category) {
        LinearLayout targetLayout;
        switch (category) {
            case "Bills":
                targetLayout = billsLayout;
                break;
            case "Grocery":
                targetLayout = groceryLayout;
                break;
            default:
                targetLayout = othersLayout;
                break;
        }

        View expenseView = createExpenseItemView(document);
        targetLayout.addView(expenseView);
    }

    private View createExpenseItemView(QueryDocumentSnapshot document) {
        if (getContext() == null) return new View(requireActivity());
        LinearLayout container = new LinearLayout(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 16);
        container.setLayoutParams(params);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(android.view.Gravity.CENTER_VERTICAL);
        container.setPadding(32, 24, 32, 24);
        container.setBackgroundResource(R.drawable.item_background_rounded);

        // Paid checkbox
        android.widget.CheckBox paidCheckBox = new android.widget.CheckBox(getContext());
        Boolean isPaid = document.getBoolean("paid");
        paidCheckBox.setChecked(isPaid != null && isPaid);
        paidCheckBox.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView expenseText = new TextView(getContext());
        expenseText.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        ));
        
        String title = document.getString("title");
        Double amountVal = document.getDouble("amount");
        double amount = amountVal != null ? amountVal : 0.0;
        totalAmount += amount;
        
        expenseText.setText(String.format(java.util.Locale.getDefault(), "%s: \u20b1%.2f", title, amount));
        expenseText.setTextSize(16);
        expenseText.setTypeface(null, android.graphics.Typeface.BOLD);

        int textColor = android.graphics.Color.BLACK;
        if (getContext() != null) {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            if (getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)) {
                textColor = typedValue.data;
            }
        }

        // Apply strikethrough if paid
        if (isPaid != null && isPaid) {
            expenseText.setPaintFlags(expenseText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            expenseText.setTextColor(0xFF888888);
        } else {
            expenseText.setTextColor(textColor);
        }

        // Toggle paid status on checkbox change
        paidCheckBox.setOnClickListener(v -> {
            boolean checked = paidCheckBox.isChecked();
            db.collection("expenses").document(document.getId())
                .update("paid", checked)
                .addOnSuccessListener(aVoid -> loadExpenses())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show());
        });
        
        ImageButton deleteButton = new ImageButton(getContext());
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete);
        deleteButton.setBackgroundResource(android.R.color.transparent);
        deleteButton.setVisibility(View.GONE);
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(document.getId()));
        
        container.addView(paidCheckBox);
        container.addView(expenseText);
        container.addView(deleteButton);
        
        container.setOnClickListener(v -> showEditExpenseDialog(document));
        
        return container;
    }

    private void updateTotalAmount() {
        if (totalAmountText != null) {
            totalAmountText.setText(String.format(java.util.Locale.getDefault(), "Total: ₱%.2f", totalAmount));
        }
        if (perPersonText != null) {
            double perPerson = memberCount > 0 ? totalAmount / memberCount : totalAmount;
            perPersonText.setText(String.format(java.util.Locale.getDefault(), "Your share: ₱%.2f (%d members)", perPerson, memberCount));
        }
    }

    private void showAddExpenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        
        EditText titleInput = dialogView.findViewById(R.id.expenseTitleInput);
        EditText amountInput = dialogView.findViewById(R.id.expenseAmountInput);
        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
            R.array.expense_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        
        builder.setView(dialogView)
            .setTitle("Add Expense")
            .setPositiveButton("Add", (dialog, which) -> {
                String title = titleInput.getText().toString();
                String amountStr = amountInput.getText().toString();
                String category = categorySpinner.getSelectedItem().toString();
                
                if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(amountStr)) {
                    double amount = Double.parseDouble(amountStr);
                    addExpense(title, amount, category);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void addExpense(String title, double amount, String category) {
        Map<String, Object> expense = new HashMap<>();
        expense.put("title", title);
        expense.put("amount", amount);
        expense.put("category", category);
        expense.put("groupId", groupId);
        expense.put("createdBy", currentUser.getUid());
        
        db.collection("expenses")
            .add(expense)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(getContext(), "Expense added successfully", Toast.LENGTH_SHORT).show();
                loadExpenses();
            })
            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add expense", Toast.LENGTH_SHORT).show());
    }

    private void showNoGroupMessage() {
        Toast.makeText(getContext(), "You are not part of any group", Toast.LENGTH_LONG).show();
    }

    private void showDeleteConfirmationDialog(String expenseId) {
        new AlertDialog.Builder(getContext())
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Yes", (dialog, which) -> deleteExpense(expenseId))
            .setNegativeButton("No", null)
            .show();
    }

    private void deleteExpense(String expenseId) {
        db.collection("expenses").document(expenseId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Expense deleted", Toast.LENGTH_SHORT).show();
                loadExpenses();
            })
            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete expense", Toast.LENGTH_SHORT).show());
    }

    private void showEditExpenseDialog(QueryDocumentSnapshot document) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        
        EditText titleInput = dialogView.findViewById(R.id.expenseTitleInput);
        EditText amountInput = dialogView.findViewById(R.id.expenseAmountInput);
        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        
        // Set current values
        titleInput.setText(document.getString("title"));
        amountInput.setText(String.valueOf(document.getDouble("amount")));
        
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
            R.array.expense_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        
        String currentCategory = document.getString("category");
        if (currentCategory != null) {
            int spinnerPosition = adapter.getPosition(currentCategory);
            categorySpinner.setSelection(spinnerPosition);
        }
        
        builder.setView(dialogView)
            .setTitle("Edit Expense")
            .setPositiveButton("Update", (dialog, which) -> {
                String title = titleInput.getText().toString();
                String amountStr = amountInput.getText().toString();
                String category = categorySpinner.getSelectedItem().toString();
                
                if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(amountStr)) {
                    double amount = Double.parseDouble(amountStr);
                    updateExpense(document.getId(), title, amount, category);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateExpense(String expenseId, String title, double amount, String category) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("amount", amount);
        updates.put("category", category);
        
        db.collection("expenses").document(expenseId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Expense updated", Toast.LENGTH_SHORT).show();
                loadExpenses();
            })
            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update expense", Toast.LENGTH_SHORT).show());
    }

}