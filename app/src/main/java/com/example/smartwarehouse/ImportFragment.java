package com.example.smartwarehouse;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ImportFragment extends Fragment {
    private static final String TAG = "ImportFragment";
    private FirebaseFirestore db;
    private EditText editSoCode, editItemCode, editQuantity, editExportDate;
    private Spinner spinnerBrand;
    private Button btnScanQr, btnImport, btnSaveQr, btnPrintQr;
    private ImageView imgQrCode;
    private LinearLayout qrActions;
    private String lastImportedItemId;
    private Bitmap lastQrBitmap;
    private String lastSoCode, lastItemCode, lastQuantity, lastImportDate, lastExportDate;
    private static final int SHELF_CAPACITY = 100; // Giới hạn của mỗi kệ là 100 items
    private ArrayAdapter<String> brandAdapter;
    private List<String> brandList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import, container, false);

        db = FirebaseFirestore.getInstance();
        editSoCode = view.findViewById(R.id.edit_so_code);
        editItemCode = view.findViewById(R.id.edit_item_code);
        editQuantity = view.findViewById(R.id.edit_quantity);
        editExportDate = view.findViewById(R.id.edit_export_date);
        spinnerBrand = view.findViewById(R.id.spinner_brand);
        btnScanQr = view.findViewById(R.id.btn_scan_qr);
        btnImport = view.findViewById(R.id.btn_import);
        btnSaveQr = view.findViewById(R.id.btn_save_qr);
        btnPrintQr = view.findViewById(R.id.btn_print_qr);
        imgQrCode = view.findViewById(R.id.img_qr_code);
        qrActions = view.findViewById(R.id.qr_actions);

        // Thiết lập Spinner cho thương hiệu từ Firestore
        setupBrandSpinner();

        // Thiết lập DatePicker
        editExportDate.setOnClickListener(v -> showDatePickerDialog());

        // Thiết lập quét mã QR
        btnScanQr.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan QR Code");
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(true);
            options.setOrientationLocked(true);
            options.setCaptureActivity(CustomCaptureActivity.class);
            barcodeLauncher.launch(options);
        });

        // Nút nhập hàng
        btnImport.setOnClickListener(v -> importItem());

        // Nút lưu mã QR
        btnSaveQr.setOnClickListener(v -> saveQrCode());

        // Nút in mã QR (mock)
        btnPrintQr.setOnClickListener(v -> Toast.makeText(requireContext(), "Printing QR Code (mock)", Toast.LENGTH_SHORT).show());

        return view;
    }

    // Thiết lập Spinner cho thương hiệu từ Firestore
    private void setupBrandSpinner() {
        brandList = new ArrayList<>();
        brandAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, brandList);
        brandAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBrand.setAdapter(brandAdapter);

        // Lắng nghe thay đổi thời gian thực từ Firestore
        db.collection("brands").addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Lỗi tải thương hiệu: " + e.getMessage());
                Toast.makeText(requireContext(), "Lỗi tải thương hiệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            brandList.clear();
            if (queryDocumentSnapshots != null) {
                for (var doc : queryDocumentSnapshots) {
                    String brandCode = doc.getString("brand_code");
                    if (brandCode != null) {
                        brandList.add(brandCode);
                    }
                }
            }
            brandAdapter.notifyDataSetChanged();
            if (brandList.isEmpty()) {
                Toast.makeText(requireContext(), "Không có thương hiệu nào!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Đăng ký launcher cho quét mã QR
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    String qrData = result.getContents();
                    String[] parts = qrData.split(":");
                    if (parts.length == 5) {
                        String soCode = parts[0];
                        String itemCode = parts[1];
                        String quantity = parts[2];
                        String importDate = parts[3];
                        String expectedExportDate = parts[4];

                        editSoCode.setText(soCode);
                        editItemCode.setText(itemCode.length() >= 3 ? itemCode.substring(3) : itemCode);
                        editQuantity.setText(quantity);
                        editExportDate.setText(expectedExportDate);
                        String brand = itemCode.length() >= 3 ? itemCode.substring(0, 3) : "";
                        int brandIndex = brandList.indexOf(brand);
                        spinnerBrand.setSelection(brandIndex != -1 ? brandIndex : 0);

                        db.collection("items")
                                .whereEqualTo("SO_code", soCode)
                                .whereEqualTo("item_code", itemCode)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        var doc = queryDocumentSnapshots.getDocuments().get(0);
                                        long currentQuantity = doc.getLong("quantity");
                                        editQuantity.setText(String.valueOf(currentQuantity));
                                        editExportDate.setText(doc.getString("expected_export_date"));
                                    } else {
                                        editQuantity.setText("");
                                        editExportDate.setText("");
                                    }
                                });
                    } else if (parts.length == 2) {
                        String soCode = parts[0];
                        String itemCode = parts[1];
                        editSoCode.setText(soCode);
                        editItemCode.setText(itemCode.length() >= 3 ? itemCode.substring(3) : itemCode);
                        String brand = itemCode.length() >= 3 ? itemCode.substring(0, 3) : "";
                        int brandIndex = brandList.indexOf(brand);
                        spinnerBrand.setSelection(brandIndex != -1 ? brandIndex : 0);

                        db.collection("items")
                                .whereEqualTo("SO_code", soCode)
                                .whereEqualTo("item_code", itemCode)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        var doc = queryDocumentSnapshots.getDocuments().get(0);
                                        long currentQuantity = doc.getLong("quantity");
                                        editQuantity.setText(String.valueOf(currentQuantity));
                                        editExportDate.setText(doc.getString("expected_export_date"));
                                    } else {
                                        editQuantity.setText("");
                                        editExportDate.setText("");
                                    }
                                });
                    } else {
                        Toast.makeText(requireContext(), "Invalid QR code format", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    editExportDate.setText(sdf.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void importItem() {
        String soCode = editSoCode.getText().toString().trim();
        String brand = spinnerBrand.getSelectedItem() != null ? spinnerBrand.getSelectedItem().toString() : "";
        String itemCode = brand + editItemCode.getText().toString().trim();
        String quantityStr = editQuantity.getText().toString().trim();
        String exportDate = editExportDate.getText().toString().trim();

        if (soCode.isEmpty() || itemCode.length() != 11 || quantityStr.isEmpty() || exportDate.isEmpty() || brand.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields correctly", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Kiểm tra đầu vào thất bại: soCode=" + soCode + ", itemCode=" + itemCode + ", quantity=" + quantityStr + ", exportDate=" + exportDate + ", brand=" + brand);
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid quantity format", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Lỗi parse quantity: " + e.getMessage());
            return;
        }

        // Lưu thông tin để tạo mã QR
        lastSoCode = soCode;
        lastItemCode = itemCode;
        lastQuantity = quantityStr;
        lastImportDate = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
        lastExportDate = exportDate;

        Log.d(TAG, "Chuẩn bị nhập hàng: soCode=" + lastSoCode + ", itemCode=" + lastItemCode + ", quantity=" + lastQuantity + ", importDate=" + lastImportDate + ", exportDate=" + lastExportDate);

        // Kiểm tra xem mặt hàng đã tồn tại chưa
        db.collection("items")
                .whereEqualTo("SO_code", soCode)
                .whereEqualTo("item_code", itemCode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Cập nhật mặt hàng hiện có
                        var doc = queryDocumentSnapshots.getDocuments().get(0);
                        String docId = doc.getId();
                        long currentQuantity = doc.getLong("quantity");
                        long newQuantity = currentQuantity + quantity;

                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("quantity", newQuantity);
                        updateData.put("status", "in_stock");
                        updateData.put("import_date", lastImportDate);
                        updateData.put("expected_export_date", exportDate);

                        db.collection("items").document(docId)
                                .update(updateData)
                                .addOnSuccessListener(aVoid -> {
                                    lastImportedItemId = docId;
                                    Toast.makeText(requireContext(), "Item updated, quantity: " + newQuantity, Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Cập nhật mặt hàng thành công, gọi generateAndShowQrCode");
                                    generateAndShowQrCode(docId);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(requireContext(), "Failed to update item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Lỗi cập nhật mặt hàng: " + e.getMessage());
                                });
                    } else {
                        // Tạo mặt hàng mới
                        Map<String, Object> item = new HashMap<>();
                        item.put("SO_code", soCode);
                        item.put("item_code", itemCode);
                        item.put("import_date", lastImportDate);
                        item.put("expected_export_date", exportDate);
                        item.put("quantity", quantity);

                        // Phân phối vào kệ phù hợp
                        selectShelf(shelfId -> {
                            if (shelfId == null) {
                                Toast.makeText(requireContext(), "No available shelf found", Toast.LENGTH_SHORT).show();
                                Log.w(TAG, "Không tìm thấy kệ trống");
                                return;
                            }

                            item.put("shelf_id", shelfId);
                            item.put("status", "in_stock");

                            db.collection("items").add(item)
                                    .addOnSuccessListener(documentReference -> {
                                        lastImportedItemId = documentReference.getId();
                                        Toast.makeText(requireContext(), "Item imported to " + shelfId, Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "Tạo mặt hàng mới thành công, gọi generateAndShowQrCode");
                                        generateAndShowQrCode(lastImportedItemId);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(requireContext(), "Failed to import item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Lỗi tạo mặt hàng mới: " + e.getMessage());
                                    });
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to check item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Lỗi truy vấn items: " + e.getMessage());
                });
    }

    // Phương thức chọn kệ phù hợp
    private void selectShelf(ShelfSelectionCallback callback) {
        db.collection("shelves").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onShelfSelected(null);
                        Log.w(TAG, "Không có kệ nào trong Firestore");
                        return;
                    }

                    List<String> shelfIds = new ArrayList<>();
                    for (var doc : queryDocumentSnapshots) {
                        String shelfNumber = doc.getString("shelf_number");
                        if (shelfNumber != null) {
                            shelfIds.add(shelfNumber);
                        }
                    }

                    if (shelfIds.isEmpty()) {
                        callback.onShelfSelected(null);
                        Log.w(TAG, "Danh sách shelfIds rỗng");
                        return;
                    }

                    // Tính số lượng items trên từng kệ
                    AtomicLong pendingQueries = new AtomicLong(shelfIds.size());
                    Map<String, Long> shelfQuantities = new HashMap<>();

                    for (String shelfId : shelfIds) {
                        db.collection("items")
                                .whereEqualTo("shelf_id", shelfId)
                                .whereEqualTo("status", "in_stock")
                                .get()
                                .addOnSuccessListener(itemSnapshots -> {
                                    long totalQuantity = 0;
                                    for (var itemDoc : itemSnapshots) {
                                        Long quantity = itemDoc.getLong("quantity");
                                        if (quantity != null) {
                                            totalQuantity += quantity;
                                        }
                                    }
                                    shelfQuantities.put(shelfId, totalQuantity);

                                    if (pendingQueries.decrementAndGet() == 0) {
                                        // Chọn kệ có ít mục hàng nhất và dưới giới hạn 100 items
                                        String selectedShelf = null;
                                        long minQuantity = Long.MAX_VALUE;

                                        for (Map.Entry<String, Long> entry : shelfQuantities.entrySet()) {
                                            long qty = entry.getValue();
                                            if (qty < SHELF_CAPACITY && qty < minQuantity) {
                                                minQuantity = qty;
                                                selectedShelf = entry.getKey();
                                            }
                                        }

                                        callback.onShelfSelected(selectedShelf);
                                        Log.d(TAG, "Kệ được chọn: " + selectedShelf);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (pendingQueries.decrementAndGet() == 0) {
                                        callback.onShelfSelected(null);
                                        Log.e(TAG, "Lỗi truy vấn items cho kệ: " + e.getMessage());
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onShelfSelected(null);
                    Log.e(TAG, "Lỗi truy vấn shelves: " + e.getMessage());
                });
    }

    // Callback để trả về kệ được chọn
    interface ShelfSelectionCallback {
        void onShelfSelected(String shelfId);
    }

    // Tạo và hiển thị mã QR
    private void generateAndShowQrCode(String itemId) {
        Log.d(TAG, "Bắt đầu tạo mã QR cho itemId: " + itemId);
        String qrContent = String.format("%s:%s:%s:%s:%s",
                lastSoCode, lastItemCode, lastQuantity, lastImportDate, lastExportDate);
        Log.d(TAG, "Nội dung QR: " + qrContent);

        if (lastSoCode == null || lastItemCode == null || lastQuantity == null || lastImportDate == null || lastExportDate == null) {
            Toast.makeText(requireContext(), "Dữ liệu QR không hợp lệ!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Dữ liệu QR null: soCode=" + lastSoCode + ", itemCode=" + lastItemCode + ", quantity=" + lastQuantity +
                    ", importDate=" + lastImportDate + ", exportDate=" + lastExportDate);
            return;
        }

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300);
            lastQrBitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.RGB_565);
            for (int x = 0; x < 300; x++) {
                for (int y = 0; y < 300; y++) {
                    lastQrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            imgQrCode.setImageBitmap(lastQrBitmap);
            imgQrCode.setVisibility(View.VISIBLE);
            qrActions.setVisibility(View.VISIBLE);
            View cardQrCode = getView().findViewById(R.id.card_qr_code);
            if (cardQrCode != null) {
                cardQrCode.setVisibility(View.VISIBLE);
            }
            Toast.makeText(requireContext(), "Mã QR đã được tạo!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Mã QR được tạo và hiển thị thành công");
        } catch (WriterException e) {
            Toast.makeText(requireContext(), "Failed to generate QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Lỗi tạo mã QR: " + e.getMessage());
        }
    }

    private void saveQrCode() {
        if (lastQrBitmap == null) {
            Toast.makeText(requireContext(), "No QR code to save", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Không có mã QR để lưu");
            return;
        }

        String fileName = "QR_" + lastImportedItemId + ".png";
        try {
            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SmartWarehouse");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                var uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    outputStream = requireContext().getContentResolver().openOutputStream(uri);
                    lastQrBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();

                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    requireContext().getContentResolver().update(uri, values, null, null);

                    Toast.makeText(requireContext(), "QR code saved to Pictures/SmartWarehouse/" + fileName, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Mã QR lưu thành công tại: " + fileName);
                } else {
                    throw new Exception("Failed to create MediaStore URI");
                }
            } else {
                if (requireContext().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                    Log.d(TAG, "Yêu cầu quyền WRITE_EXTERNAL_STORAGE");
                    return;
                }

                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SmartWarehouse");
                if (!directory.exists() && !directory.mkdirs()) {
                    throw new Exception("Failed to create directory: " + directory.getAbsolutePath());
                }

                File file = new File(directory, fileName);
                outputStream = new FileOutputStream(file);
                lastQrBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(android.net.Uri.fromFile(file));
                requireContext().sendBroadcast(mediaScanIntent);

                Toast.makeText(requireContext(), "QR code saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                Log.d(TAG, "Mã QR lưu thành công tại: " + file.getAbsolutePath());
            }

            imgQrCode.setVisibility(View.GONE);
            qrActions.setVisibility(View.GONE);
            View cardQrCode = getView().findViewById(R.id.card_qr_code);
            if (cardQrCode != null) {
                cardQrCode.setVisibility(View.GONE);
            }
            lastQrBitmap = null;
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to save QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Lỗi lưu mã QR: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveQrCode();
            Log.d(TAG, "Quyền WRITE_EXTERNAL_STORAGE được cấp, thử lưu mã QR lại");
        } else {
            Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Quyền WRITE_EXTERNAL_STORAGE bị từ chối");
        }
    }
}