package com.example.smartwarehouse;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class HomeFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvUserInfo, tvWarehouseName, tvTotalShelves, tvShelvesNearFull, tvTotalBrands, tvBrandsLowStock,
            tvItemsNearDeadline, tvTotalItems, tvImportsToday, tvExportsToday;
    private PieChart pieChart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvUserInfo = view.findViewById(R.id.tv_user_info);
        tvWarehouseName = view.findViewById(R.id.tv_warehouse_name);
        tvTotalShelves = view.findViewById(R.id.tv_total_shelves);
        tvShelvesNearFull = view.findViewById(R.id.tv_shelves_near_full);
        tvTotalBrands = view.findViewById(R.id.tv_total_brands);
        tvBrandsLowStock = view.findViewById(R.id.tv_brands_low_stock);
        tvItemsNearDeadline = view.findViewById(R.id.tv_items_near_deadline);
        tvTotalItems = view.findViewById(R.id.tv_total_items);
        tvImportsToday = view.findViewById(R.id.tv_imports_today);
        tvExportsToday = view.findViewById(R.id.tv_exports_today);
        pieChart = view.findViewById(R.id.pie_chart);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllData();
    }

    private void loadAllData() {
        loadUserInfo();
        loadWarehouseData();
        loadShelvesData();
        loadBrandsData();
        loadItemsNearDeadline();
        loadPieChartData();
    }

    private void loadUserInfo() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId != null) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String role = documentSnapshot.getString("role");
                            tvUserInfo.setText("Welcome, " + username + " (" + role + ")");
                        } else {
                            tvUserInfo.setText("Welcome, Guest (Employee)");
                        }
                    })
                    .addOnFailureListener(e -> tvUserInfo.setText("Welcome, Guest (Employee)"));
        } else {
            tvUserInfo.setText("Welcome, Guest (Employee)");
        }
    }

    private void loadWarehouseData() {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());

        // Load warehouse name
        db.collection("warehouses").document("main_warehouse").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        tvWarehouseName.setText("Warehouse: " + (name != null ? name : "Main Warehouse"));
                    } else {
                        tvWarehouseName.setText("Warehouse: Main Warehouse");
                    }
                });

        // Load total items
        db.collection("items").whereEqualTo("status", "in_stock").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long totalItems = 0;
                    for (var doc : queryDocumentSnapshots) {
                        Long quantity = doc.getLong("quantity");
                        if (quantity != null) {
                            totalItems += quantity;
                        }
                    }
                    tvTotalItems.setText(String.valueOf(totalItems));
                });

        // Load imports today
        db.collection("items").whereEqualTo("import_date", today).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long importsToday = 0;
                    for (var doc : queryDocumentSnapshots) {
                        Long quantity = doc.getLong("quantity");
                        if (quantity != null) {
                            importsToday += quantity;
                        }
                    }
                    tvImportsToday.setText(String.valueOf(importsToday));
                });

        // Load exports today
        db.collection("export_history").whereEqualTo("export_date", today).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long exportsToday = 0;
                    for (var doc : queryDocumentSnapshots) {
                        Long quantity = doc.getLong("quantity_exported");
                        if (quantity != null) {
                            exportsToday += quantity;
                        }
                    }
                    tvExportsToday.setText(String.valueOf(exportsToday));
                });
    }

    private void loadShelvesData() {
        db.collection("shelves").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long totalShelves = queryDocumentSnapshots.size();
                    tvTotalShelves.setText(String.valueOf(totalShelves));

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvShelvesNearFull.setText("0");
                        return;
                    }

                    AtomicLong nearFullCount = new AtomicLong(0);
                    AtomicLong pendingQueries = new AtomicLong(queryDocumentSnapshots.size());

                    for (var doc : queryDocumentSnapshots) {
                        String shelfNumber = doc.getString("shelf_number");
                        if (shelfNumber != null) {
                            calculateShelfQuantity(shelfNumber, count -> {
                                if (count > 80) {
                                    nearFullCount.incrementAndGet();
                                }
                                if (pendingQueries.decrementAndGet() == 0) {
                                    tvShelvesNearFull.setText(String.valueOf(nearFullCount.get()));
                                }
                            });
                        } else {
                            if (pendingQueries.decrementAndGet() == 0) {
                                tvShelvesNearFull.setText(String.valueOf(nearFullCount.get()));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvTotalShelves.setText("0");
                    tvShelvesNearFull.setText("0");
                });
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

    private void loadBrandsData() {
        db.collection("brands").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long totalBrands = queryDocumentSnapshots.size();
                    tvTotalBrands.setText(String.valueOf(totalBrands));

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvBrandsLowStock.setText("0");
                        return;
                    }

                    Map<String, Long> brandQuantities = new HashMap<>();
                    for (var doc : queryDocumentSnapshots) {
                        String brandCode = doc.getString("brand_code");
                        if (brandCode != null) {
                            brandQuantities.put(brandCode, 0L);
                        }
                    }

                    db.collection("items").whereEqualTo("status", "in_stock").get()
                            .addOnSuccessListener(itemSnapshots -> {
                                AtomicLong lowStockCount = new AtomicLong(0);
                                for (var doc : itemSnapshots) {
                                    String itemCode = doc.getString("item_code");
                                    Long quantity = doc.getLong("quantity");
                                    if (itemCode != null && itemCode.length() >= 3 && quantity != null) {
                                        String brandCode = itemCode.substring(0, 3);
                                        if (brandQuantities.containsKey(brandCode)) {
                                            brandQuantities.put(brandCode, brandQuantities.get(brandCode) + quantity);
                                        }
                                    }
                                }

                                for (long quantity : brandQuantities.values()) {
                                    if (quantity > 0 && quantity < 10) {
                                        lowStockCount.incrementAndGet();
                                    }
                                }
                                tvBrandsLowStock.setText(String.valueOf(lowStockCount.get()));
                            })
                            .addOnFailureListener(e -> tvBrandsLowStock.setText("0"));
                })
                .addOnFailureListener(e -> {
                    tvTotalBrands.setText("0");
                    tvBrandsLowStock.setText("0");
                });
    }

    private void loadItemsNearDeadline() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 3);
        String deadlineDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        db.collection("items").whereEqualTo("status", "in_stock").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long nearDeadlineCount = 0;
                    for (var doc : queryDocumentSnapshots) {
                        String expectedExportDate = doc.getString("expected_export_date");
                        if (expectedExportDate != null) {
                            try {
                                Date exportDate = sdf.parse(expectedExportDate);
                                Date deadline = sdf.parse(deadlineDate);
                                if (exportDate != null && exportDate.before(deadline) && exportDate.after(new Date())) {
                                    nearDeadlineCount++;
                                }
                            } catch (ParseException e) {
                                // Ignore invalid dates
                            }
                        }
                    }
                    tvItemsNearDeadline.setText(String.valueOf(nearDeadlineCount));
                })
                .addOnFailureListener(e -> tvItemsNearDeadline.setText("0"));
    }

    private void loadPieChartData() {
        db.collection("brands").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<PieEntry> entries = new ArrayList<>();
                    Map<String, Float> brandQuantities = new HashMap<>();

                    for (var doc : queryDocumentSnapshots) {
                        String brandCode = doc.getString("brand_code");
                        if (brandCode != null) {
                            brandQuantities.put(brandCode, 0f);
                        }
                    }

                    db.collection("items").whereEqualTo("status", "in_stock").get()
                            .addOnSuccessListener(itemSnapshots -> {
                                for (var doc : itemSnapshots) {
                                    String itemCode = doc.getString("item_code");
                                    Long quantity = doc.getLong("quantity");
                                    if (itemCode != null && itemCode.length() >= 3 && quantity != null) {
                                        String brandCode = itemCode.substring(0, 3);
                                        if (brandQuantities.containsKey(brandCode)) {
                                            brandQuantities.put(brandCode, brandQuantities.get(brandCode) + quantity);
                                        }
                                    }
                                }

                                for (Map.Entry<String, Float> entry : brandQuantities.entrySet()) {
                                    if (entry.getValue() > 0) {
                                        entries.add(new PieEntry(entry.getValue(), entry.getKey()));
                                    }
                                }

                                if (entries.isEmpty()) {
                                    Toast.makeText(requireContext(), "No items in stock to display", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                int[] colors = new int[]{
                                        Color.rgb(255, 99, 71),
                                        Color.rgb(60, 179, 113),
                                        Color.rgb(65, 105, 225),
                                        Color.rgb(255, 215, 0),
                                        Color.rgb(138, 43, 226),
                                        Color.rgb(255, 165, 0)
                                };
                                ArrayList<Integer> colorList = new ArrayList<>();
                                for (int i = 0; i < entries.size(); i++) {
                                    colorList.add(colors[i % colors.length]);
                                }

                                PieDataSet dataSet = new PieDataSet(entries, "Brands");
                                dataSet.setColors(colorList);
                                dataSet.setValueTextColor(Color.BLACK);
                                dataSet.setValueTextSize(12f);

                                PieData data = new PieData(dataSet);
                                pieChart.setData(data);
                                pieChart.setEntryLabelColor(Color.BLACK);
                                pieChart.setEntryLabelTextSize(12f);
                                pieChart.invalidate();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load brands: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}