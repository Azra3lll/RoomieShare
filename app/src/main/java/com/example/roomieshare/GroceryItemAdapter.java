package com.example.roomieshare;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.roomieshare.models.GroceryItem;
import java.util.List;

public class GroceryItemAdapter extends RecyclerView.Adapter<GroceryItemAdapter.GroceryViewHolder> {
    private Context context;
    private List<GroceryItem> groceryList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onCheckChanged(GroceryItem item, boolean isChecked);
        void onDeleteClick(GroceryItem item);
    }

    public GroceryItemAdapter(Context context, List<GroceryItem> groceryList, OnItemClickListener listener) {
        this.context = context;
        this.groceryList = groceryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroceryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_grocery, parent, false);
        return new GroceryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroceryViewHolder holder, int position) {
        GroceryItem item = groceryList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return groceryList.size();
    }

    class GroceryViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkboxPurchased;
        TextView textItemName;
        TextView textAddedBy;
        ImageButton buttonDelete;

        public GroceryViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxPurchased = itemView.findViewById(R.id.checkboxPurchased);
            textItemName = itemView.findViewById(R.id.textItemName);
            textAddedBy = itemView.findViewById(R.id.textAddedBy);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);

            checkboxPurchased.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCheckChanged(groceryList.get(position), checkboxPurchased.isChecked());
                }
            });

            buttonDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeleteClick(groceryList.get(position));
                }
            });
        }

        public void bind(GroceryItem item) {
            textItemName.setText(item.getName());
            
            if (item.getAssigneeName() != null && !item.getAssigneeName().isEmpty()) {
                textAddedBy.setText("Added by " + item.getAssigneeName());
            } else {
                textAddedBy.setText("Added by someone");
            }

            // Temporarily remove listener to avoid triggering it when setting the state programmatically
            checkboxPurchased.setOnCheckedChangeListener(null);
            checkboxPurchased.setChecked(item.isPurchased());
            
            // Re-apply click listener logic
            checkboxPurchased.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCheckChanged(groceryList.get(position), checkboxPurchased.isChecked());
                }
            });

            // Resolve theme colors dynamically
            int colorOnSurface = android.graphics.Color.BLACK;
            int colorOnSurfaceVariant = android.graphics.Color.GRAY;
            if (context != null) {
                android.util.TypedValue typedValue = new android.util.TypedValue();
                if (context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)) {
                    colorOnSurface = typedValue.data;
                }
                if (context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)) {
                    colorOnSurfaceVariant = typedValue.data;
                }
            }

            if (item.isPurchased()) {
                textItemName.setPaintFlags(textItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textItemName.setTextColor(colorOnSurfaceVariant);
            } else {
                textItemName.setPaintFlags(textItemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                textItemName.setTextColor(colorOnSurface);
            }
        }
    }
}
