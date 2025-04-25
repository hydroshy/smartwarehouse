package com.example.smartwarehouse;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import android.app.AlertDialog;

public class SettingsFragment extends Fragment {

    private Button changePasswordButton;
    private Button logoutButton;
    private Button switchLanguageButton;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        mAuth = FirebaseAuth.getInstance();
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        switchLanguageButton = view.findViewById(R.id.switchLanguageButton);

        // Khi nhấn nút Đổi mật khẩu
        changePasswordButton.setOnClickListener(v -> {
            if (!NetworkUtil.isNetworkAvailable(getActivity())) {
                showNetworkWarningDialog();
                return;
            }
            startActivity(new Intent(getActivity(), ChangePasswordActivity.class));
        });

        // Khi nhấn nút Đăng xuất
        logoutButton.setOnClickListener(v -> {
            if (!NetworkUtil.isNetworkAvailable(getActivity())) {
                showNetworkWarningDialog();
                return;
            }
            mAuth.signOut();
            Toast.makeText(getActivity(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finishAffinity();
        });

        // Khi nhấn nút Chuyển đổi ngôn ngữ
        switchLanguageButton.setOnClickListener(v -> {
            showLanguageSelectionDialog();
        });

        return view;
    }

    // Phương thức hiển thị Dialog cảnh báo mất mạng
    private void showNetworkWarningDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.no_internet_title)
                .setMessage(R.string.no_internet_message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    // Phương thức hiển thị Dialog chọn ngôn ngữ
    private void showLanguageSelectionDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.language_dialog_title)
                .setItems(R.array.languages, (dialog, which) -> {
                    String languageCode = which == 0 ? "en" : "vi"; // 0: English, 1: Vietnamese
                    LanguageUtil.setLocale(getActivity(), languageCode);
                    // Làm mới Activity để áp dụng ngôn ngữ
                    Intent intent = getActivity().getIntent();
                    getActivity().finish();
                    startActivity(intent);
                })
                .setCancelable(true)
                .show();
    }
}