package com.example.smartwarehouse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ExportHistoryFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TableLayout tableExportHistory;
    private TextView tvAccessDenied;
    private LinearLayout layoutContent;
    private ArrayList<Item> items;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_export_history, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tableExportHistory = view.findViewById(R.id.table_export_history);
        tvAccessDenied = view.findViewById(R.id.tv_access_denied);
        layoutContent = view.findViewById(R.id.layout_content);

        items = new ArrayList<>();

        if (checkGooglePlayServices()) {
            checkUserRole();
        } else {
            layoutContent.setVisibility(View.GONE);
            tvAccessDenied.setVisibility(View.VISIBLE);
            tvAccessDenied.setText(getString(R.string.google_play_services_not_available));
        }

        return view;
    }

    private boolean checkGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(requireContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(requireActivity(), resultCode, 9000).show();
            } else {
                Toast.makeText(requireContext(), getString(R.string.device_does_not_support_google_play_services), Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
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
                                loadExportHistory();
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

    private void loadExportHistory() {
        db.collection("export_history")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    items.clear();
                    for (var doc : queryDocumentSnapshots) {
                        String itemCode = doc.getString("item_code");
                        String soCode = doc.getString("SO_code");
                        String exportDate = doc.getString("export_date");
                        Long quantity = doc.getLong("quantity_exported");
                        if (itemCode != null && quantity != null) {
                            items.add(new Item(itemCode, soCode, null, null, exportDate, quantity));
                        }
                    }
                    if (items.isEmpty()) {
                        Toast.makeText(requireContext(), getString(R.string.no_exported_items_found), Toast.LENGTH_SHORT).show();
                    } else {
                        displayExportHistory();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), getString(R.string.failed_to_save_export_history, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayExportHistory() {
        // Xóa các hàng cũ (trừ hàng tiêu đề)
        tableExportHistory.removeViews(1, tableExportHistory.getChildCount() - 1);

        // Thêm các hàng cho từng đơn hàng đã xuất kho
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            TableRow row = new TableRow(requireContext());
            row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            row.setPadding(8, 12, 8, 12);
            row.setBackgroundResource(android.R.drawable.list_selector_background);

            // Màu sắc xen kẽ cho các hàng
            if (i % 2 == 0) {
                row.setBackgroundColor(getResources().getColor(android.R.color.white));
            } else {
                row.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }

            TextView tvItemCode = new TextView(requireContext());
            tvItemCode.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
            tvItemCode.setText(item.getItemCode());
            tvItemCode.setTextSize(14);
            tvItemCode.setTextColor(getResources().getColor(android.R.color.black));
            tvItemCode.setPadding(0, 0, 8, 0);

            TextView tvSoCode = new TextView(requireContext());
            tvSoCode.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
            tvSoCode.setText(item.getSoCode() != null ? item.getSoCode() : getString(R.string.not_available));
            tvSoCode.setTextSize(14);
            tvSoCode.setTextColor(getResources().getColor(android.R.color.black));
            tvSoCode.setPadding(0, 0, 8, 0);

            TextView tvExportDate = new TextView(requireContext());
            tvExportDate.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
            tvExportDate.setText(item.getExpectedExportDate() != null ? item.getExpectedExportDate() : getString(R.string.not_available));
            tvExportDate.setTextSize(14);
            tvExportDate.setTextColor(getResources().getColor(android.R.color.black));
            tvExportDate.setPadding(0, 0, 8, 0);

            TextView tvQuantity = new TextView(requireContext());
            tvQuantity.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
            tvQuantity.setText(String.valueOf(item.getQuantity()));
            tvQuantity.setTextSize(14);
            tvQuantity.setTextColor(getResources().getColor(android.R.color.black));

            row.addView(tvItemCode);
            row.addView(tvSoCode);
            row.addView(tvExportDate);
            row.addView(tvQuantity);

            tableExportHistory.addView(row);
        }
    }
}