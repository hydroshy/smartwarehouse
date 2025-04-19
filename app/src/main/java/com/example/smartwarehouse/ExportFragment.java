package com.example.smartwarehouse;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ExportFragment extends Fragment {
    private FirebaseFirestore db;
    private Button btnScanQr, btnPickImage, btnExport;
    private TableLayout tableProductDetails;
    private TextView tvSoCode, tvItemCode, tvQuantity, tvImportDate, tvExpectedExportDate;
    private String currentSoCode, currentItemCode;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_export, container, false);

        db = FirebaseFirestore.getInstance();
        btnScanQr = view.findViewById(R.id.btn_scan_qr);
        btnPickImage = view.findViewById(R.id.btn_pick_image);
        btnExport = view.findViewById(R.id.btn_export);
        tableProductDetails = view.findViewById(R.id.table_product_details);
        tvSoCode = view.findViewById(R.id.tv_so_code);
        tvItemCode = view.findViewById(R.id.tv_item_code);
        tvQuantity = view.findViewById(R.id.tv_quantity);
        tvImportDate = view.findViewById(R.id.tv_import_date);
        tvExpectedExportDate = view.findViewById(R.id.tv_expected_export_date);

        btnScanQr.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan QR Code");
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setBarcodeImageEnabled(true);
            options.setCaptureActivity(CustomCaptureActivity.class); // Sử dụng CustomCaptureActivity để khóa hướng
            barcodeLauncher.launch(options);
        });

        btnPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnExport.setOnClickListener(v -> showExportDialog());

        return view;
    }

    private void processQrCode(String qrData) {
        String[] parts = qrData.split(":");
        String soCode, itemCode;

        if (parts.length == 5) {
            // Định dạng từ ImportFragment: SO_code:item_code:quantity:import_date:expected_export_date
            soCode = parts[0];
            itemCode = parts[1];
        } else if (parts.length == 2) {
            // Định dạng cũ: SO_code:item_code
            soCode = parts[0];
            itemCode = parts[1];
        } else {
            Toast.makeText(requireContext(), "Invalid QR code format", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("items")
                .whereEqualTo("SO_code", soCode)
                .whereEqualTo("item_code", itemCode)
                .whereEqualTo("status", "in_stock")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        var doc = queryDocumentSnapshots.getDocuments().get(0);
                        String docId = doc.getId();
                        currentSoCode = soCode;
                        currentItemCode = itemCode;

                        // Display product details
                        tvSoCode.setText(soCode);
                        tvItemCode.setText(itemCode);
                        tvQuantity.setText(String.valueOf(doc.getLong("quantity")));
                        tvImportDate.setText(doc.getString("import_date"));
                        tvExpectedExportDate.setText(doc.getString("expected_export_date"));

                        tableProductDetails.setVisibility(View.VISIBLE);
                        btnExport.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(requireContext(), "Item not found or out of stock", Toast.LENGTH_SHORT).show();
                        tableProductDetails.setVisibility(View.GONE);
                        btnExport.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tableProductDetails.setVisibility(View.GONE);
                    btnExport.setVisibility(View.GONE);
                });
    }

    private void showExportDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_export_quantity, null);
        EditText editQuantity = dialogView.findViewById(R.id.edit_quantity);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setTitle("Export Item")
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String quantityStr = editQuantity.getText().toString().trim();
            if (quantityStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            int exportQuantity = Integer.parseInt(quantityStr);
            if (exportQuantity <= 0) {
                Toast.makeText(requireContext(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("items")
                    .whereEqualTo("SO_code", currentSoCode)
                    .whereEqualTo("item_code", currentItemCode)
                    .whereEqualTo("status", "in_stock")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            var doc = queryDocumentSnapshots.getDocuments().get(0);
                            String docId = doc.getId();
                            long currentQuantity = doc.getLong("quantity");

                            if (exportQuantity > currentQuantity) {
                                Toast.makeText(requireContext(), "Export quantity exceeds available stock", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            long newQuantity = currentQuantity - exportQuantity;
                            Map<String, Object> updateData = new HashMap<>();
                            updateData.put("quantity", newQuantity);
                            if (newQuantity == 0) {
                                updateData.put("status", "out_of_stock");
                            }

                            db.collection("items").document(docId)
                                    .update(updateData)
                                    .addOnSuccessListener(aVoid -> {
                                        Map<String, Object> history = new HashMap<>();
                                        history.put("SO_code", currentSoCode);
                                        history.put("item_code", currentItemCode);
                                        history.put("quantity_exported", exportQuantity);
                                        history.put("export_date", new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime()));

                                        db.collection("export_history").add(history)
                                                .addOnSuccessListener(docRef -> {
                                                    Toast.makeText(requireContext(), "Exported " + exportQuantity + " units", Toast.LENGTH_SHORT).show();
                                                    tableProductDetails.setVisibility(View.GONE);
                                                    btnExport.setVisibility(View.GONE);
                                                    dialog.dismiss();
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to save export history: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to export item: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
        });

        dialog.show();
    }
}