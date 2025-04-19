package com.example.smartwarehouse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class ShelfAdapter extends RecyclerView.Adapter<ShelfAdapter.ShelfViewHolder> {
    private ArrayList<Shelf> shelves;
    private OnShelfClickListener clickListener;
    private OnDeleteClickListener deleteClickListener;
    private OnEditClickListener editClickListener;

    public interface OnShelfClickListener {
        void onShelfClick(String shelfNumber);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(String shelfNumber);
    }

    public interface OnEditClickListener {
        void onEditClick(Shelf shelf);
    }

    public ShelfAdapter(ArrayList<Shelf> shelves, OnShelfClickListener clickListener,
                        OnDeleteClickListener deleteClickListener, OnEditClickListener editClickListener) {
        this.shelves = shelves;
        this.clickListener = clickListener;
        this.deleteClickListener = deleteClickListener;
        this.editClickListener = editClickListener;
    }

    @NonNull
    @Override
    public ShelfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng item_shelf_manager.xml cho ManagerFragment
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shelf_manager, parent, false);
        return new ShelfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShelfViewHolder holder, int position) {
        Shelf shelf = shelves.get(position);
        holder.tvShelfNumber.setText(shelf.getShelfNumber());
        holder.tvShelfQuantity.setText(shelf.getCurrentQuantity() + "/100");

        holder.itemView.setOnClickListener(v -> clickListener.onShelfClick(shelf.getShelfNumber()));
        holder.btnDelete.setOnClickListener(v -> deleteClickListener.onDeleteClick(shelf.getShelfNumber()));
        holder.itemView.setOnLongClickListener(v -> {
            editClickListener.onEditClick(shelf);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return shelves.size();
    }

    public void updateShelves(ArrayList<Shelf> newShelves) {
        this.shelves = newShelves;
        notifyDataSetChanged();
    }

    static class ShelfViewHolder extends RecyclerView.ViewHolder {
        ImageView ivShelfIcon;
        TextView tvShelfNumber, tvShelfQuantity;
        MaterialButton btnDelete;

        ShelfViewHolder(@NonNull View itemView) {
            super(itemView);
            ivShelfIcon = itemView.findViewById(R.id.iv_shelf_icon);
            tvShelfNumber = itemView.findViewById(R.id.tv_shelf_number);
            tvShelfQuantity = itemView.findViewById(R.id.tv_shelf_quantity);
            btnDelete = itemView.findViewById(R.id.btn_delete_shelf);
        }
    }
}