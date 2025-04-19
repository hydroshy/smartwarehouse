package com.example.smartwarehouse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class BrandAdapter extends RecyclerView.Adapter<BrandAdapter.BrandViewHolder> {
    private ArrayList<Brand> brands;
    private OnDeleteClickListener deleteClickListener;
    private OnEditClickListener editClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(String brandCode);
    }

    public interface OnEditClickListener {
        void onEditClick(Brand brand);
    }

    public BrandAdapter(ArrayList<Brand> brands, OnDeleteClickListener deleteClickListener, OnEditClickListener editClickListener) {
        this.brands = brands;
        this.deleteClickListener = deleteClickListener;
        this.editClickListener = editClickListener;
    }

    @NonNull
    @Override
    public BrandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brand, parent, false);
        return new BrandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BrandViewHolder holder, int position) {
        Brand brand = brands.get(position);
        holder.tvBrandCode.setText(brand.getBrandCode());
        holder.btnDelete.setOnClickListener(v -> deleteClickListener.onDeleteClick(brand.getBrandCode()));
        holder.itemView.setOnLongClickListener(v -> {
            editClickListener.onEditClick(brand);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return brands.size();
    }

    public void updateBrands(ArrayList<Brand> newBrands) {
        this.brands = newBrands;
        notifyDataSetChanged();
    }

    static class BrandViewHolder extends RecyclerView.ViewHolder {
        TextView tvBrandCode;
        MaterialButton btnDelete;

        BrandViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBrandCode = itemView.findViewById(R.id.tv_brand_code);
            btnDelete = itemView.findViewById(R.id.btn_delete_brand);
        }
    }
}