package com.example.roomieshare;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.roomieshare.models.GroceryItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GroceryListFragment extends Fragment implements GroceryItemAdapter.OnItemClickListener {
    private RecyclerView recyclerViewToBuy;
    private RecyclerView recyclerViewPurchased;
    private GroceryItemAdapter toBuyAdapter;
    private GroceryItemAdapter purchasedAdapter;
    private List<GroceryItem> toBuyList;
    private List<GroceryItem> purchasedList;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String currentGroupId;
    private String currentUserDisplayName;
    private ListenerRegistration groceryListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grocery_list, container, false);

        recyclerViewToBuy = view.findViewById(R.id.recyclerViewToBuy);
        recyclerViewPurchased = view.findViewById(R.id.recyclerViewPurchased);

        toBuyList = new ArrayList<>();
        purchasedList = new ArrayList<>();

        toBuyAdapter = new GroceryItemAdapter(getContext(), toBuyList, this);
        purchasedAdapter = new GroceryItemAdapter(getContext(), purchasedList, this);

        recyclerViewToBuy.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewToBuy.setAdapter(toBuyAdapter);

        recyclerViewPurchased.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPurchased.setAdapter(purchasedAdapter);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        FloatingActionButton fabAddGrocery = view.findViewById(R.id.fabAddGrocery);
        fabAddGrocery.setOnClickListener(v -> showAddGroceryDialog());

        if (currentUser != null) {
            fetchUserGroupInfo();
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().finish();
            }
        }

        return view;
    }

    private void fetchUserGroupInfo() {
        db.collection("users").document(currentUser.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    currentGroupId = documentSnapshot.getString("groupId");
                    
                    String display = documentSnapshot.getString("username");
                    if (display == null || display.isEmpty()) {
                        display = documentSnapshot.getString("displayName");
                    }
                    if (display == null || display.isEmpty()) {
                        display = "Someone";
                    }
                    currentUserDisplayName = display;

                    if (currentGroupId != null) {
                        listenToGroceryItems();
                    } else {
                        Toast.makeText(getContext(), "Join a group to share groceries", Toast.LENGTH_LONG).show();
                    }
                }
            });
    }

    private void listenToGroceryItems() {
        if (currentGroupId == null || db == null) return;
        
        if (groceryListener != null) {
            groceryListener.remove();
        }

        groceryListener = db.collection("grocery_items")
            .whereEqualTo("groupId", currentGroupId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Toast.makeText(getContext(), "Error loading groceries: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (value != null) {
                    toBuyList.clear();
                    purchasedList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        GroceryItem item = doc.toObject(GroceryItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            if (item.isPurchased()) {
                                purchasedList.add(item);
                            } else {
                                toBuyList.add(item);
                            }
                        }
                    }

                    // After loading grocery items, also load grocery expenses into the list
                    loadGroceryExpenses();
                }
            });
    }

    private void showAddGroceryDialog() {
        if (getContext() == null || currentGroupId == null) {
            if (currentGroupId == null) {
                Toast.makeText(getContext(), "You must be in a group to add items", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_grocery_item, null);
        EditText editGroceryName = dialogView.findViewById(R.id.editGroceryName);

        new AlertDialog.Builder(getContext())
            .setTitle("Add Grocery Item")
            .setView(dialogView)
            .setPositiveButton("Add", (dialog, which) -> {
                String name = editGroceryName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(getContext(), "Item name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                addGroceryItem(name);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void addGroceryItem(String name) {
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("name", name);
        itemData.put("addedBy", currentUser.getUid());
        itemData.put("assigneeName", currentUserDisplayName);
        itemData.put("groupId", currentGroupId);
        itemData.put("isPurchased", false);
        itemData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("grocery_items").add(itemData)
            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add item", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onCheckChanged(GroceryItem item, boolean isChecked) {
        if (item.getId() != null) {
            // Check if this is an expense-sourced item (id starts with "expense_")
            if (item.getId().startsWith("expense_")) {
                String expenseId = item.getId().substring("expense_".length());
                db.collection("expenses").document(expenseId)
                    .update("paid", isChecked)
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update expense", Toast.LENGTH_SHORT).show());
            } else {
                db.collection("grocery_items").document(item.getId())
                    .update("isPurchased", isChecked)
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update item", Toast.LENGTH_SHORT).show());
            }
        }
    }

    @Override
    public void onDeleteClick(GroceryItem item) {
        if (item.getId() != null) {
            String displayName = item.getName();
            boolean isExpense = item.getId().startsWith("expense_");
            String docId = isExpense ? item.getId().substring("expense_".length()) : item.getId();
            String collection = isExpense ? "expenses" : "grocery_items";

            new AlertDialog.Builder(getContext())
                .setTitle("Delete Item")
                .setMessage("Remove '" + displayName + "' from the list?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection(collection).document(docId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            // Refresh the list
                            if (isExpense) {
                                listenToGroceryItems();
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete item", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groceryListener != null) {
            groceryListener.remove();
        }
    }

    private void loadGroceryExpenses() {
        if (currentGroupId == null) return;

        db.collection("expenses")
            .whereEqualTo("groupId", currentGroupId)
            .whereEqualTo("category", "Grocery")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!isAdded() || getContext() == null) return;

                if (querySnapshot != null) {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        Double amountVal = doc.getDouble("amount");
                        double amount = amountVal != null ? amountVal : 0.0;
                        Boolean isPaid = doc.getBoolean("paid");

                        // Create a GroceryItem to represent this expense
                        GroceryItem expenseItem = new GroceryItem();
                        expenseItem.setId("expense_" + doc.getId());
                        expenseItem.setName(String.format(java.util.Locale.getDefault(), "%s: \u20b1%.2f", title, amount));
                        expenseItem.setAssigneeName("Expense");
                        expenseItem.setPurchased(isPaid != null && isPaid);
                        expenseItem.setGroupId(currentGroupId);

                        if (isPaid != null && isPaid) {
                            purchasedList.add(expenseItem);
                        } else {
                            toBuyList.add(expenseItem);
                        }
                    }
                }

                toBuyAdapter.notifyDataSetChanged();
                purchasedAdapter.notifyDataSetChanged();
            });
    }

}
