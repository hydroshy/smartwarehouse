package com.example.smartwarehouse;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ManagerFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextInputEditText editWarehouseName, editBrandCode, editShelfNumber;
    private MaterialButton btnUpdateWarehouse, btnAddBrand, btnAddShelf;
    private RecyclerView recyclerEmployees, recyclerBrands, recyclerShelves;
    private EmployeeAdapter employeeAdapter;
    private BrandAdapter brandAdapter;
    private ShelfAdapter shelfAdapter;
    private ArrayList<User> employeeList;
    private ArrayList<Brand> brandList;
    private ArrayList<Shelf> shelfList;
    private TextView tvAccessDenied;
    private LinearLayout layoutManagerControls;
    private boolean isManager = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        editWarehouseName = view.findViewById(R.id.edit_warehouse_name);
        editBrandCode = view.findViewById(R.id.edit_brand_code);
        editShelfNumber = view.findViewById(R.id.edit_shelf_number);
        btnUpdateWarehouse = view.findViewById(R.id.btn_update_warehouse);
        btnAddBrand = view.findViewById(R.id.btn_add_brand);
        btnAddShelf = view.findViewById(R.id.btn_add_shelf);
        recyclerEmployees = view.findViewById(R.id.recycler_employees);
        recyclerBrands = view.findViewById(R.id.recycler_brands);
        recyclerShelves = view.findViewById(R.id.recycler_shelves);
        tvAccessDenied = view.findViewById(R.id.tv_access_denied);
        layoutManagerControls = view.findViewById(R.id.layout_manager_controls);

        employeeList = new ArrayList<>();
        brandList = new ArrayList<>();
        shelfList = new ArrayList<>();

        employeeAdapter = new EmployeeAdapter(employeeList, this::showDeleteConfirmationDialogForEmployee, this::editEmployee);
        recyclerEmployees.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerEmployees.setAdapter(employeeAdapter);

        brandAdapter = new BrandAdapter(brandList, this::showDeleteConfirmationDialogForBrand, this::editBrand);
        recyclerBrands.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerBrands.setAdapter(brandAdapter);

        shelfAdapter = new ShelfAdapter(shelfList, this::onShelfClick, this::showDeleteConfirmationDialogForShelf, this::editShelf);
        recyclerShelves.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerShelves.setAdapter(shelfAdapter);

        checkUserRole();
        loadWarehouseName();

        btnUpdateWarehouse.setOnClickListener(v -> updateWarehouseName());
        btnAddBrand.setOnClickListener(v -> addBrand());
        btnAddShelf.setOnClickListener(v -> addShelf());

        return view;
    }

    private void checkUserRole() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId != null) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            isManager = "manager".equals(role);
                            if (isManager) {
                                layoutManagerControls.setVisibility(View.VISIBLE);
                                tvAccessDenied.setVisibility(View.GONE);
                                loadEmployees();
                                loadBrands();
                                loadShelves();
                            } else {
                                layoutManagerControls.setVisibility(View.GONE);
                                tvAccessDenied.setVisibility(View.VISIBLE);
                            }
                        } else {
                            layoutManagerControls.setVisibility(View.GONE);
                            tvAccessDenied.setVisibility(View.VISIBLE);
                            Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        layoutManagerControls.setVisibility(View.GONE);
                        tvAccessDenied.setVisibility(View.VISIBLE);
                        Toast.makeText(requireContext(), "Failed to load user role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            layoutManagerControls.setVisibility(View.GONE);
            tvAccessDenied.setVisibility(View.VISIBLE);
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWarehouseName() {
        db.collection("warehouses").document("main_warehouse").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        editWarehouseName.setText(name != null ? name : "");
                    }
                });
    }

    private void updateWarehouseName() {
        if (!isManager) {
            Toast.makeText(requireContext(), "Only managers can perform this action", Toast.LENGTH_SHORT).show();
            return;
        }

        String newName = editWarehouseName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a warehouse name", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> warehouse = new HashMap<>();
        warehouse.put("name", newName);
        db.collection("warehouses").document("main_warehouse").set(warehouse)
                .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Warehouse name updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update warehouse name: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addBrand() {
        if (!isManager) {
            Toast.makeText(requireContext(), "Only managers can perform this action", Toast.LENGTH_SHORT).show();
            return;
        }

        String brandCode = editBrandCode.getText().toString().trim().toUpperCase();
        if (brandCode.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a brand code", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> brand = new HashMap<>();
        brand.put("brand_code", brandCode);
        brand.put("brand_name", brandCode);

        db.collection("brands").document(brandCode).set(brand)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Brand added", Toast.LENGTH_SHORT).show();
                    editBrandCode.setText("");
                    loadBrands();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to add brand: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmationDialogForBrand(String brandCode) {
        if (!isManager) {
            Toast.makeText(requireContext(), "Only managers can perform this action", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_confirm_delete);

        TextView tvConfirmMessage = dialog.findViewById(R.id.tv_confirm_message);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm);

        tvConfirmMessage.setText("Are you sure you want to delete brand " + brandCode + "?");

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            deleteBrand(brandCode);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteBrand(String brandCode) {
        db.collection("brands").document(brandCode).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Brand deleted", Toast.LENGTH_SHORT).show();
                    loadBrands();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to delete brand: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void editBrand(Brand brand) {
        if (!isManager) {
            Toast.makeText(requireContext(), "Only managers can perform this action", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_edit_brand);

        TextInputEditText editBrandCode = dialog.findViewById(R.id.edit_brand_code);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save);

        editBrandCode.setText(brand.getBrandCode());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newBrandCode = editBrandCode.getText().toString().trim().toUpperCase();
            if (newBrandCode.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a brand code", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newBrandCode.equals(brand.getBrandCode())) {
                Map<String, Object> updatedBrand = new HashMap<>();
                updatedBrand.put("brand_code", newBrandCode);
                updatedBrand.put("brand_name", newBrandCode);

                db.collection("brands").document(brand.getBrandCode()).delete()
                        .addOnSuccessListener(aVoid -> {
                            db.collection("brands").document(newBrandCode).set(updatedBrand)
                                    .addOnSuccessListener(aVoid2 -> {
                                        Toast.makeText(requireContext(), "Brand updated", Toast.LENGTH_SHORT).show();
                                        loadBrands();
                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update brand: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update brand: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void loadBrands() {
        db.collection("brands").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    brandList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        String brandCode = doc.getString("brand_code");
                        if (brandCode != null) {
                            brandList.add(new Brand(brandCode));
                        }
                    }
                    brandAdapter.updateBrands(brandList);
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load brands: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addShelf() {
        if (!isManager) {
            Toast.makeText(requireContext(), "Only managers can perform this action", Toast.LENGTH_SHORT).show();
            return;
        }

        String shelfNumber = editShelfNumber.getText().toString().trim();
        if (shelfNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a shelf number", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> shelf = new HashMap<>();
        shelf.put("shelf_number", shelfNumber);

        db.collection("shelves").document(shelfNumber).set(shelf)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Shelf added", Toast.LENGTH_SHORT).show();
                    editShelfNumber.setText("");
                    loadShelves();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to add shelf: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void onShelfClick(String shelfNumber) {
        Toast.makeText(requireContext(), "Clicked on shelf: " + shelfNumber, Toast.LENGTH_SHORT).show();
        // Bạn có thể thêm logic chuyển hướng hoặc hiển thị chi tiết kệ tại đây
    }

    private void showDeleteConfirmationDialogForShelf(String shelfNumber) {
        if (!isManager) {
            Toast.makeText(requireContext(), "Only managers can perform this action", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_confirm_delete);

        TextView tvConfirmMessage = dialog.findViewById(R.id.tv_confirm_message);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm);

        tvConfirmMessage.setText("Are you sure you want to delete shelf " + shelfNumber + "?");

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            deleteShelf(shelfNumber);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteShelf(String shelfNumber) {
        db.collection("shelves").document(shelfNumber).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Shelf deleted", Toast.LENGTH_SHORT).show();
                    loadShelves();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to delete shelf: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void editShelf(Shelf shelf) {
        if (!isManager) {
            Toast.makeText(requireContext(), "Only managers can perform this action", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_edit_shelf);

        TextInputEditText editShelfNumber = dialog.findViewById(R.id.edit_shelf_number);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save);

        editShelfNumber.setText(shelf.getShelfNumber());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newShelfNumber = editShelfNumber.getText().toString().trim();
            if (newShelfNumber.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a shelf number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newShelfNumber.equals(shelf.getShelfNumber())) {
                Map<String, Object> updatedShelf = new HashMap<>();
                updatedShelf.put("shelf_number", newShelfNumber);

                db.collection("shelves").document(shelf.getShelfNumber()).delete()
                        .addOnSuccessListener(aVoid -> {
                            db.collection("shelves").document(newShelfNumber).set(updatedShelf)
                                    .addOnSuccessListener(aVoid2 -> {
                                        // Cập nhật shelf_id trong items
                                        db.collection("items")
                                                .whereEqualTo("shelf_id", shelf.getShelfNumber())
                                                .get()
                                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                                    for (var doc : queryDocumentSnapshots) {
                                                        db.collection("items").document(doc.getId())
                                                                .update("shelf_id", newShelfNumber);
                                                    }
                                                    Toast.makeText(requireContext(), "Shelf updated", Toast.LENGTH_SHORT).show();
                                                    loadShelves();
                                                    dialog.dismiss();
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update shelf: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update shelf: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update shelf: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void loadShelves() {
        db.collection("shelves").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    shelfList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        shelfAdapter.updateShelves(shelfList);
                        return;
                    }

                    AtomicLong pendingQueries = new AtomicLong(queryDocumentSnapshots.size());

                    for (var doc : queryDocumentSnapshots) {
                        String shelfNumber = doc.getString("shelf_number");
                        if (shelfNumber != null) {
                            calculateShelfQuantity(shelfNumber, quantity -> {
                                shelfList.add(new Shelf(shelfNumber, quantity));
                                if (pendingQueries.decrementAndGet() == 0) {
                                    shelfAdapter.updateShelves(shelfList);
                                }
                            });
                        } else {
                            if (pendingQueries.decrementAndGet() == 0) {
                                shelfAdapter.updateShelves(shelfList);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load shelves: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void calculateShelfQuantity(String shelfNumber, QuantityCallback callback) {
        db.collection("items")
                .whereEqualTo("shelf_id", shelfNumber)
                .whereEqualTo("status", "in_stock")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long totalQuantity = 0;
                    for (var doc : queryDocumentSnapshots) {
                        Long quantity = doc.getLong("quantity");
                        if (quantity != null) {
                            totalQuantity += quantity;
                        }
                    }
                    callback.onQuantityCalculated(totalQuantity);
                })
                .addOnFailureListener(e -> callback.onQuantityCalculated(0));
    }

    interface QuantityCallback {
        void onQuantityCalculated(long quantity);
    }

    private void loadEmployees() {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    employeeList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        String userId = doc.getId();
                        String username = doc.getString("username");
                        String email = doc.getString("email");
                        String role = doc.getString("role");
                        if (username != null && email != null && role != null) {
                            employeeList.add(new User(userId, username, email, role));
                        }
                    }
                    employeeAdapter.updateEmployees(employeeList);
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load employees: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmationDialogForEmployee(String userId) {
        if (!isManager) {
            Toast.makeText(requireContext(), "Only managers can perform this action", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_confirm_delete);

        TextView tvConfirmMessage = dialog.findViewById(R.id.tv_confirm_message);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm);

        tvConfirmMessage.setText("Are you sure you want to delete this employee?");

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            deleteEmployee(userId);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteEmployee(String userId) {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId.equals(currentUserId)) {
            Toast.makeText(requireContext(), "Cannot delete yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if ("manager".equals(role)) {
                            Toast.makeText(requireContext(), "Cannot delete another manager", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        db.collection("users").document(userId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(requireContext(), "Employee deleted", Toast.LENGTH_SHORT).show();
                                    loadEmployees();
                                })
                                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to delete employee: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to check employee role: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void editEmployee(User user) {
        if (!isManager) {
            Toast.makeText(requireContext(), "Only managers can perform this action", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_edit_employee);

        TextInputEditText editUsername = dialog.findViewById(R.id.edit_username);
        TextInputEditText editEmail = dialog.findViewById(R.id.edit_email);
        TextInputEditText editRole = dialog.findViewById(R.id.edit_role);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save);

        editUsername.setText(user.getUsername());
        editEmail.setText(user.getEmail());
        editRole.setText(user.getRole());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newUsername = editUsername.getText().toString().trim();
            String newEmail = editEmail.getText().toString().trim();
            String newRole = editRole.getText().toString().trim().toLowerCase();

            if (newUsername.isEmpty() || newEmail.isEmpty() || newRole.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newRole.equals("employee") && !newRole.equals("manager")) {
                Toast.makeText(requireContext(), "Role must be 'employee' or 'manager'", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updatedUser = new HashMap<>();
            updatedUser.put("username", newUsername);
            updatedUser.put("email", newEmail);
            updatedUser.put("role", newRole);

            db.collection("users").document(user.getUserId()).update(updatedUser)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Employee updated", Toast.LENGTH_SHORT).show();
                        loadEmployees();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update employee: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }
}