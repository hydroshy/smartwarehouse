package com.example.smartwarehouse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class WarehouseManagementFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TableLayout tableShelves;
    private TextView tvItemsTitle;
    private TextView tvAccessDenied;
    private LinearLayout layoutContent;
    private ListView listItems;
    private ArrayAdapter<Item> itemAdapter;
    private ArrayList<Shelf> shelves;
    private ArrayList<Item> items;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_warehouse_management, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tableShelves = view.findViewById(R.id.table_shelves);
        tvItemsTitle = view.findViewById(R.id.tv_items_title);
        tvAccessDenied = view.findViewById(R.id.tv_access_denied);
        layoutContent = view.findViewById(R.id.layout_content);
        listItems = view.findViewById(R.id.list_items);

        shelves = new ArrayList<>();
        items = new ArrayList<>();

        itemAdapter = new ArrayAdapter<Item>(requireContext(), R.layout.item_warehouse_item, R.id.tv_item_code, items) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Item item = getItem(position);
                TextView tvItemCode = view.findViewById(R.id.tv_item_code);
                TextView tvSoCode = view.findViewById(R.id.tv_so_code);
                TextView tvImportDate = view.findViewById(R.id.tv_import_date);
                TextView tvExpectedExportDate = view.findViewById(R.id.tv_expected_export_date);
                TextView tvQuantity = view.findViewById(R.id.tv_quantity);

                tvItemCode.setText(item.getItemCode());
                tvSoCode.setText(getString(R.string.so_code_label) + " " + (item.getSoCode() != null ? item.getSoCode() : getString(R.string.not_available)));
                tvImportDate.setText(getString(R.string.import_date_label) + " " + (item.getImportDate() != null ? item.getImportDate() : getString(R.string.not_available)));
                tvExpectedExportDate.setText(getString(R.string.export_date_label) + " " + (item.getExpectedExportDate() != null ? item.getExpectedExportDate() : getString(R.string.not_available)));
                tvQuantity.setText(getString(R.string.quantity_label) + " " + item.getQuantity());

                return view;
            }
        };
        listItems.setAdapter(itemAdapter);

        checkUserRole();

        return view;
    }

    private void checkUserRole() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId != null) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            if ("manager".equals(role) || "employee".equals(role)) {
                                layoutContent.setVisibility(View.VISIBLE);
                                tvAccessDenied.setVisibility(View.GONE);
                                loadShelves();
                            } else {
                                layoutContent.setVisibility(View.GONE);
                                tvAccessDenied.setVisibility(View.VISIBLE);
                            }
                        } else {
                            layoutContent.setVisibility(View.GONE);
                            tvAccessDenied.setVisibility(View.VISIBLE);
                            Toast.makeText(requireContext(), getString(R.string.user_data_not_found), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        layoutContent.setVisibility(View.GONE);
                        tvAccessDenied.setVisibility(View.VISIBLE);
                        Toast.makeText(requireContext(), getString(R.string.failed_to_load_user_role, e.getMessage()), Toast.LENGTH_SHORT).show();
                    });
        } else {
            layoutContent.setVisibility(View.GONE);
            tvAccessDenied.setVisibility(View.VISIBLE);
            Toast.makeText(requireContext(), getString(R.string.user_not_logged_in), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadShelves() {
        db.collection("shelves").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    shelves.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        return;
                    }

                    AtomicLong pendingQueries = new AtomicLong(queryDocumentSnapshots.size());

                    for (var doc : queryDocumentSnapshots) {
                        String shelfNumber = doc.getString("shelf_number");
                        if (shelfNumber != null) {
                            calculateShelfQuantity(shelfNumber, (quantity, earliestExportDate) -> {
                                shelves.add(new Shelf(shelfNumber, quantity, earliestExportDate));
                                if (pendingQueries.decrementAndGet() == 0) {
                                    displayShelves();
                                }
                            });
                        } else {
                            if (pendingQueries.decrementAndGet() == 0) {
                                displayShelves();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), getString(R.string.failed_to_load_shelves, e.getMessage()), Toast.LENGTH_SHORT).show());
    }

    private void calculateShelfQuantity(String shelfNumber, ShelfDataCallback callback) {
        db.collection("items")
                .whereEqualTo("shelf_id", shelfNumber)
                .whereEqualTo("status", "in_stock")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long totalQuantity = 0;
                    String earliestExportDate = null;

                    for (var doc : queryDocumentSnapshots) {
                        Long quantity = doc.getLong("quantity");
                        String expectedExportDate = doc.getString("expected_export_date");
                        if (quantity != null) {
                            totalQuantity += quantity;
                        }
                        if (expectedExportDate != null) {
                            if (earliestExportDate == null || expectedExportDate.compareTo(earliestExportDate) < 0) {
                                earliestExportDate = expectedExportDate;
                            }
                        }
                    }
                    callback.onShelfDataCalculated(totalQuantity, earliestExportDate);
                })
                .addOnFailureListener(e -> callback.onShelfDataCalculated(0, null));
    }

    interface ShelfDataCallback {
        void onShelfDataCalculated(long quantity, String earliestExportDate);
    }

    private void displayShelves() {
        // Xóa các hàng cũ
        tableShelves.removeAllViews();

        // Thêm các hàng cho từng kệ
        for (int i = 0; i < shelves.size(); i++) {
            Shelf shelf = shelves.get(i);
            TableRow row = new TableRow(requireContext());
            row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            // Màu sắc xen kẽ cho các hàng
            if (i % 2 == 0) {
                row.setBackgroundColor(getResources().getColor(android.R.color.white));
            } else {
                row.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }

            // Inflate giao diện item_shelf.xml
            View shelfView = LayoutInflater.from(requireContext()).inflate(R.layout.item_shelf, row, false);
            ImageView ivShelfIcon = shelfView.findViewById(R.id.iv_shelf_icon);
            TextView tvShelfNumber = shelfView.findViewById(R.id.tv_shelf_number);
            TextView tvShelfQuantity = shelfView.findViewById(R.id.tv_shelf_quantity);
            TextView tvShelfStatus = shelfView.findViewById(R.id.tv_shelf_status);
            TextView tvExpectedExportDate = shelfView.findViewById(R.id.tv_expected_export_date);

            // Gán dữ liệu
            tvShelfNumber.setText(shelf.getShelfNumber());
            tvShelfQuantity.setText(shelf.getCurrentQuantity() + "/100");

            // Hiển thị trạng thái
            String status;
            int statusColor;
            if (shelf.getCurrentQuantity() == 100) {
                status = getString(R.string.full_status);
                statusColor = getResources().getColor(android.R.color.holo_red_dark);
            } else if (shelf.getCurrentQuantity() >= 80) {
                status = getString(R.string.near_full_status);
                statusColor = getResources().getColor(android.R.color.holo_orange_dark);
            } else {
                status = getString(R.string.normal_status);
                statusColor = getResources().getColor(android.R.color.black);
            }
            tvShelfStatus.setText(status);
            tvShelfStatus.setTextColor(statusColor);

            // Hiển thị ngày xuất kho sớm nhất
            tvExpectedExportDate.setText(shelf.getEarliestExpectedExportDate());

            row.addView(shelfView);

            row.setOnClickListener(v -> loadItemsForShelf(shelf.getShelfNumber()));

            tableShelves.addView(row);
        }
    }

    private void loadItemsForShelf(String shelfNumber) {
        db.collection("items")
                .whereEqualTo("shelf_id", shelfNumber)
                .whereEqualTo("status", "in_stock")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    items.clear();
                    for (var doc : queryDocumentSnapshots) {
                        String itemCode = doc.getString("item_code");
                        String soCode = doc.getString("SO_code");
                        String importDate = doc.getString("import_date");
                        String expectedExportDate = doc.getString("expected_export_date");
                        Long quantity = doc.getLong("quantity");
                        if (itemCode != null && quantity != null) {
                            items.add(new Item(itemCode, soCode, importDate, expectedExportDate, null, quantity));
                        }
                    }
                    if (items.isEmpty()) {
                        tvItemsTitle.setVisibility(View.GONE);
                        listItems.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), getString(R.string.no_items_found_for_shelf, shelfNumber), Toast.LENGTH_SHORT).show();
                    } else {
                        tvItemsTitle.setText(getString(R.string.items_in_shelf_with_number, shelfNumber));
                        tvItemsTitle.setVisibility(View.VISIBLE);
                        listItems.setVisibility(View.VISIBLE);
                        itemAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), getString(R.string.failed_to_load_items, e.getMessage()), Toast.LENGTH_SHORT).show());
    }
}