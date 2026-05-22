package com.example.roomieshare.models;

import com.google.firebase.firestore.Exclude;

public class GroceryItem {
    @Exclude
    private String id;
    private String name;
    private String addedBy;
    private String assigneeName;
    private String groupId;
    private boolean isPurchased;

    public GroceryItem() {
        // Empty constructor needed for Firestore
    }

    public GroceryItem(String name, String addedBy, String assigneeName, String groupId, boolean isPurchased) {
        this.name = name;
        this.addedBy = addedBy;
        this.assigneeName = assigneeName;
        this.groupId = groupId;
        this.isPurchased = isPurchased;
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isPurchased() {
        return isPurchased;
    }

    public void setPurchased(boolean purchased) {
        isPurchased = purchased;
    }
}
