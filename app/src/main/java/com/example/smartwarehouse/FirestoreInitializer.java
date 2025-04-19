package com.example.smartwarehouse;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class FirestoreInitializer {
    private FirebaseFirestore db;

    public FirestoreInitializer() {
        db = FirebaseFirestore.getInstance();
    }

    public void initializeData() {
        // Kiểm tra và tạo collection users
        db.collection("users").limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Map<String, Object> user = new HashMap<>();
                        user.put("username", "SampleUser");
                        user.put("email", "sample@example.com");
                        user.put("role", "employee");
                        db.collection("users").document("sample_user").set(user);
                    }
                });

        // Kiểm tra và tạo collection items
        db.collection("items").limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        ArrayList<Map<String, Object>> sampleItems = new ArrayList<>();
                        Map<String, Object> item1 = new HashMap<>();
                        item1.put("SO_code", "SO123");
                        item1.put("item_code", "NIK12345678");
                        item1.put("import_date", new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime()));
                        item1.put("expected_export_date", "2023-10-10");
                        item1.put("quantity", 10);
                        item1.put("shelf_id", "shelf_1");
                        item1.put("status", "in_stock");
                        sampleItems.add(item1);

                        Map<String, Object> item2 = new HashMap<>();
                        item2.put("SO_code", "SO124");
                        item2.put("item_code", "PUM87654321");
                        item2.put("import_date", new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime()));
                        item2.put("expected_export_date", "2023-10-15");
                        item2.put("quantity", 5);
                        item2.put("shelf_id", "shelf_2");
                        item2.put("status", "in_stock");
                        sampleItems.add(item2);

                        for (Map<String, Object> item : sampleItems) {
                            db.collection("items").add(item);
                        }
                    }
                });

        // Kiểm tra và tạo collection shelves
        db.collection("shelves").limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        String[] shelfNumbers = {"shelf_1", "shelf_2", "shelf_3"};
                        for (String shelfNumber : shelfNumbers) {
                            Map<String, Object> shelf = new HashMap<>();
                            shelf.put("shelf_number", shelfNumber);
                            db.collection("shelves").document(shelfNumber).set(shelf);
                        }
                    }
                });

        // Kiểm tra và tạo collection brands
        db.collection("brands").limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        String[][] brands = {
                                {"NIK", "Nike"},
                                {"PUM", "Puma"},
                                {"ADI", "Adidas"}
                        };
                        for (String[] brand : brands) {
                            Map<String, Object> brandData = new HashMap<>();
                            brandData.put("brand_code", brand[0]);
                            brandData.put("brand_name", brand[1]);
                            db.collection("brands").document(brand[0]).set(brandData);
                        }
                    }
                });

        // Kiểm tra và tạo collection warehouses
        db.collection("warehouses").document("main_warehouse").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Map<String, Object> warehouse = new HashMap<>();
                        warehouse.put("name", "Main Warehouse");
                        db.collection("warehouses").document("main_warehouse").set(warehouse);
                    }
                });

        // Kiểm tra và tạo collection export_history
        db.collection("export_history").limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Map<String, Object> history = new HashMap<>();
                        history.put("SO_code", "SO123");
                        history.put("item_code", "NIK12345678");
                        history.put("quantity_exported", 5);
                        history.put("export_date", new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime()));
                        db.collection("export_history").add(history);
                    }
                });
    }
}