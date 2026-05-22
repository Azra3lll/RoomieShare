package com.example.roomieshare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChoreListAdapter extends RecyclerView.Adapter<ChoreListAdapter.ChoreViewHolder> {
    private ArrayList<Map<String, Object>> chores;
    private Context context;

    public ChoreListAdapter(Context context, List<Map<String, Object>> chores) {
        this.context = context;
        this.chores = new ArrayList<>();
        if (chores != null) {
            this.chores.addAll(chores);
        }
    }

    @NonNull
    @Override
    public ChoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chore, parent, false);
        return new ChoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChoreViewHolder holder, int position) {
        Map<String, Object> chore = chores.get(position);
        
        String title = (String) chore.get("title");
        Boolean isDone = (Boolean) chore.get("done");
        
        holder.choreTitle.setText(title != null ? title : "");
        
        // Clear the checked change listener first to prevent triggering it on recycled view holders
        holder.choreCheckBox.setOnCheckedChangeListener(null);
        holder.choreCheckBox.setChecked(isDone != null && isDone);
        
        String choreId = (String) chore.get("id");
        if (choreId != null) {
            holder.choreCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateChore(choreId, isChecked);
            });
            
            holder.deleteChoreButton.setOnClickListener(v -> deleteChore(choreId));
        } else {
            holder.deleteChoreButton.setOnClickListener(null);
        }
    }

    private void updateChore(String choreId, boolean isDone) {
        FirebaseFirestore.getInstance()
            .collection("chores")
            .document(choreId)
            .update("done", isDone)
            .addOnSuccessListener(aVoid -> showToast("Updated"))
            .addOnFailureListener(e -> showToast("Update failed"));
    }

    private void deleteChore(String choreId) {
        FirebaseFirestore.getInstance()
            .collection("chores")
            .document(choreId)
            .delete()
            .addOnSuccessListener(aVoid -> showToast("Deleted"))
            .addOnFailureListener(e -> showToast("Delete failed"));
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public void updateData(List<Map<String, Object>> newData) {
        this.chores.clear();
        if (newData != null) {
            this.chores.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return chores.size();
    }

    static class ChoreViewHolder extends RecyclerView.ViewHolder {
        TextView choreTitle;
        CheckBox choreCheckBox;
        ImageButton deleteChoreButton;

        ChoreViewHolder(View itemView) {
            super(itemView);
            choreTitle = itemView.findViewById(R.id.choreTitle);
            choreCheckBox = itemView.findViewById(R.id.choreCheckBox);
            deleteChoreButton = itemView.findViewById(R.id.deleteChoreButton);
        }
    }
} 