package com.example.smartwarehouse;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class PackageAdapter extends ArrayAdapter<Package> {
    public PackageAdapter(@NonNull Context context, ArrayList<Package> packages) {
        super(context, 0, packages);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_package, parent, false);
        }

        Package pkg = getItem(position);

        TextView tvSoCode = convertView.findViewById(R.id.tv_so_code);
        TextView tvItemCode = convertView.findViewById(R.id.tv_item_code);
        TextView tvQuantity = convertView.findViewById(R.id.tv_quantity);
        TextView tvImportDate = convertView.findViewById(R.id.tv_import_date);
        TextView tvExpectedExportDate = convertView.findViewById(R.id.tv_expected_export_date);

        if (pkg != null) {
            tvSoCode.setText(pkg.getSoCode());
            tvItemCode.setText(pkg.getItemCode());
            tvQuantity.setText(String.valueOf(pkg.getQuantity()));
            tvImportDate.setText(pkg.getImportDate());
            tvExpectedExportDate.setText(pkg.getExpectedExportDate());
        }

        return convertView;
    }
}