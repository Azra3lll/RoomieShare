package com.example.roomieshare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MessagesFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        android.widget.TextView tv = new android.widget.TextView(getContext());
        tv.setText("Messages");
        tv.setTextSize(24);
        tv.setGravity(android.view.Gravity.CENTER);
        return tv;
    }
} 