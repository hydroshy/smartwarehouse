package com.example.smartwarehouse;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import android.app.AlertDialog;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawer;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Áp dụng ngôn ngữ đã lưu trước khi giao diện được hiển thị
        LanguageUtil.setLocale(this, LanguageUtil.getCurrentLanguage(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Khởi tạo Firestore data nếu người dùng đã đăng nhập
        if (mAuth.getCurrentUser() != null) {
            FirestoreInitializer initializer = new FirestoreInitializer();
            initializer.initializeData();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    @Override
    public boolean onNavigationItemSelected(android.view.MenuItem item) {
        // Kiểm tra kết nối mạng trước khi chuyển Fragment
        if (!NetworkUtil.isNetworkAvailable(this)) {
            showNetworkWarningDialog();
            return false;
        }

        Fragment fragment = null;
        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            fragment = new HomeFragment();
        } else if (itemId == R.id.nav_import) {
            fragment = new ImportFragment();
        } else if (itemId == R.id.nav_export) {
            fragment = new ExportFragment();
        } else if (itemId == R.id.nav_order_tracking) {
            fragment = new OrderTrackingFragment();
        } else if (itemId == R.id.nav_warehouse) {
            fragment = new WarehouseManagementFragment();
        } else if (itemId == R.id.nav_export_history) {
            fragment = new ExportHistoryFragment();
        } else if (itemId == R.id.nav_manager) {
            fragment = new ManagerFragment();
        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else if (itemId == R.id.nav_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }

        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Phương thức hiển thị Dialog cảnh báo mất mạng
    private void showNetworkWarningDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.no_internet_title)
                .setMessage(R.string.no_internet_message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra mạng trước khi hiển thị Dialog
        if (!NetworkUtil.isNetworkAvailable(this)) {
            showNetworkWarningDialog();
        }
    }
}