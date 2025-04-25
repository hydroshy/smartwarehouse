package com.example.smartwarehouse;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {
    private FirebaseAuth fAuth;
    private EditText oldPasswordField, newPasswordField, confirmNewPasswordField;
    private Button changePasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        fAuth = FirebaseAuth.getInstance();
        oldPasswordField = findViewById(R.id.oldPasswordField);
        newPasswordField = findViewById(R.id.newPasswordField);
        confirmNewPasswordField = findViewById(R.id.confirmNewPasswordField);
        changePasswordButton = findViewById(R.id.changePasswordButton);

        changePasswordButton.setOnClickListener(v -> {
            String oldPassword = oldPasswordField.getText().toString().trim();
            String newPassword = newPasswordField.getText().toString().trim();
            String confirmNewPassword = confirmNewPasswordField.getText().toString().trim();

            // Kiểm tra mật khẩu mới và xác nhận có khớp không
            if (!newPassword.equals(confirmNewPassword)) {
                Toast.makeText(ChangePasswordActivity.this, "Mật khẩu mới không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra độ dài mật khẩu mới (ít nhất 6 ký tự - yêu cầu của Firebase)
            if (newPassword.length() < 6) {
                Toast.makeText(ChangePasswordActivity.this, "Mật khẩu mới phải có ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = fAuth.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                // Xác thực lại người dùng với mật khẩu cũ
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
                user.reauthenticate(credential)
                        .addOnCompleteListener(reauthTask -> {
                            if (reauthTask.isSuccessful()) {
                                // Đổi mật khẩu
                                user.updatePassword(newPassword)
                                        .addOnCompleteListener(updateTask -> {
                                            if (updateTask.isSuccessful()) {
                                                Toast.makeText(ChangePasswordActivity.this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                                                finish(); // Quay lại màn hình trước
                                            } else {
                                                Toast.makeText(ChangePasswordActivity.this, "Lỗi: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(ChangePasswordActivity.this, "Mật khẩu cũ không đúng!", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(ChangePasswordActivity.this, "Không tìm thấy người dùng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
