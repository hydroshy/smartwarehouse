package com.example.smartwarehouse;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class OrderTrackingFragment extends Fragment {
    private FirebaseFirestore db;
    private TableLayout tableOrderDetails;
    private TextInputEditText editSoCode;
    private TextView tvSoCode, tvItemCode, tvQuantity, tvImportDate, tvExpectedExportDate, tvShelfId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_tracking, container, false);

        db = FirebaseFirestore.getInstance();

        tableOrderDetails = view.findViewById(R.id.table_order_details);
        editSoCode = view.findViewById(R.id.edit_so_code);
        tvSoCode = view.findViewById(R.id.tv_so_code);
        tvItemCode = view.findViewById(R.id.tv_item_code);
        tvQuantity = view.findViewById(R.id.tv_quantity);
        tvImportDate = view.findViewById(R.id.tv_import_date);
        tvExpectedExportDate = view.findViewById(R.id.tv_expected_export_date);
        tvShelfId = view.findViewById(R.id.tv_shelf_id);

        view.findViewById(R.id.btn_scan_qr).setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan QR Code");
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setBarcodeImageEnabled(true);
            options.setCaptureActivity(CustomCaptureActivity.class);
            barcodeLauncher.launch(options);
        });

        view.findViewById(R.id.btn_pick_image).setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        view.findViewById(R.id.btn_search_so).setOnClickListener(v -> searchOrderBySoCode());

        return view;
    }

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    processQrCode(result.getContents());
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
                        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                        LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
                        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
                        Reader reader = new MultiFormatReader();
                        Result qrResult = reader.decode(binaryBitmap);
                        processQrCode(qrResult.getText());
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Failed to read QR code from image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void processQrCode(String qrData) {
        String[] parts = qrData.split(":");
        if (parts.length != 5) {
            Toast.makeText(requireContext(), "Invalid QR code format", Toast.LENGTH_SHORT).show();
            tableOrderDetails.setVisibility(View.GONE);
            return;
        }

        String soCode = parts[0];
        String itemCode = parts[1];
        String quantity = parts[2];
        String importDate = parts[3];
        String expectedExportDate = parts[4];

        searchOrderInFirestore(soCode, itemCode, quantity, importDate, expectedExportDate);
    }

    private void searchOrderBySoCode() {
        String soCode = editSoCode.getText().toString().trim();
        if (soCode.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an SO Code", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("items")
                .whereEqualTo("SO_code", soCode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        var doc = queryDocumentSnapshots.getDocuments().get(0);
                        String itemCode = doc.getString("item_code");
                        String quantity = String.valueOf(doc.getLong("quantity"));
                        String importDate = doc.getString("import_date");
                        String expectedExportDate = doc.getString("expected_export_date");

                        searchOrderInFirestore(soCode, itemCode, quantity, importDate, expectedExportDate);
                    } else {
                        Toast.makeText(requireContext(), "Order not found", Toast.LENGTH_SHORT).show();
                        tableOrderDetails.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to search order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tableOrderDetails.setVisibility(View.GONE);
                });
    }

    private void searchOrderInFirestore(String soCode, String itemCode, String quantity, String importDate, String expectedExportDate) {
        db.collection("items")
                .whereEqualTo("SO_code", soCode)
                .whereEqualTo("item_code", itemCode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        var doc = queryDocumentSnapshots.getDocuments().get(0);
                        String shelfId = doc.getString("shelf_id");

                        tvSoCode.setText(soCode);
                        tvItemCode.setText(itemCode);
                        tvQuantity.setText(quantity);
                        tvImportDate.setText(importDate);
                        tvExpectedExportDate.setText(expectedExportDate);
                        tvShelfId.setText(shelfId != null ? shelfId : "N/A");

                        tableOrderDetails.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(requireContext(), "Order not found", Toast.LENGTH_SHORT).show();
                        tableOrderDetails.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tableOrderDetails.setVisibility(View.GONE);
                });
    }
}