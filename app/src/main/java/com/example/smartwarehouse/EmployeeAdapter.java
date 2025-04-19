package com.example.smartwarehouse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {
    private ArrayList<User> employees;
    private OnDeleteClickListener deleteClickListener;
    private OnEditClickListener editClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(String userId);
    }

    public interface OnEditClickListener {
        void onEditClick(User user);
    }

    public EmployeeAdapter(ArrayList<User> employees, OnDeleteClickListener deleteClickListener, OnEditClickListener editClickListener) {
        this.employees = employees;
        this.deleteClickListener = deleteClickListener;
        this.editClickListener = editClickListener;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        User user = employees.get(position);
        holder.tvUsername.setText(user.getUsername());
        holder.tvEmail.setText(user.getEmail());
        holder.tvRole.setText("Role: " + user.getRole());

        holder.btnDelete.setOnClickListener(v -> deleteClickListener.onDeleteClick(user.getUserId()));

        holder.itemView.setOnLongClickListener(v -> {
            editClickListener.onEditClick(user);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }

    public void updateEmployees(ArrayList<User> newEmployees) {
        this.employees = newEmployees;
        notifyDataSetChanged();
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvEmail, tvRole;
        MaterialButton btnDelete;

        EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvRole = itemView.findViewById(R.id.tv_role);
            btnDelete = itemView.findViewById(R.id.btn_delete_employee);
        }
    }
}