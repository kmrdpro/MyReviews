package com.example.myreviews;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class CategoryAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<String> categories;
    private final ArrayList<Integer> productCounts;
    private final ArrayList<String> categoryIcons;

    public CategoryAdapter(
            Context context,
            ArrayList<String> categories,
            ArrayList<Integer> productCounts,
            ArrayList<String> categoryIcons
    ) {
        this.context = context;
        this.categories = categories;
        this.productCounts = productCounts;
        this.categoryIcons = categoryIcons;
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Object getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context)
                    .inflate(R.layout.item_category, parent, false);
        }

        TextView tvIcon = view.findViewById(R.id.tvCategoryIcon);
        TextView tvName = view.findViewById(R.id.tvCategoryName);
        TextView tvCount = view.findViewById(R.id.tvProductCount);

        String name = categories.get(position);
        int count = productCounts.get(position);

        tvName.setText(name);
        tvCount.setText(count + " " + getProductWord(count));

        String lowerName = name.toLowerCase(Locale.ROOT);


        String icon = categoryIcons.get(position);
        tvIcon.setText(icon);

        int iconColor;

        switch (icon) {
            case "🥤":
                iconColor = Color.rgb(58, 43, 30);
                break;
            case "🌭":
                iconColor = Color.rgb(65, 38, 42);
                break;
            case "🍬":
                iconColor = Color.rgb(57, 39, 68);
                break;
            default:
                iconColor = Color.rgb(39, 48, 65);
                break;
        }

        GradientDrawable iconBackground = new GradientDrawable();
        iconBackground.setColor(iconColor);
        iconBackground.setCornerRadius(
                14 * context.getResources().getDisplayMetrics().density
        );
        tvIcon.setBackground(iconBackground);

        return view;
    }

    private String getProductWord(int count) {
        int lastTwo = count % 100;

        if (lastTwo >= 11 && lastTwo <= 14) {
            return "товаров";
        }

        switch (count % 10) {
            case 1:
                return "товар";
            case 2:
            case 3:
            case 4:
                return "товара";
            default:
                return "товаров";
        }
    }
}
