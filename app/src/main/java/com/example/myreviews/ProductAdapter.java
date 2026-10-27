package com.example.myreviews;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class ProductAdapter extends ArrayAdapter<Product> {

    public ProductAdapter(Context context, List<Product> products) {
        super(context, 0, products);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        if (row == null) {
            row = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_product, parent, false);
        }

        ImageView thumbnail = row.findViewById(R.id.ivProductThumbnail);
        TextView name = row.findViewById(R.id.tvProductName);
        TextView rating = row.findViewById(R.id.tvProductRating);
        LinearLayout ratingBar = row.findViewById(R.id.ratingBar);

        Product product = getItem(position);

        if (product != null) {
            int score = product.getRating();

            int ratingColor;
            String ratingDescription;

            if (score <= 4) {
                ratingColor = Color.rgb(242, 139, 130);
                ratingDescription = "Не понравился";
            } else if (score <= 7) {
                ratingColor = Color.rgb(246, 200, 106);
                ratingDescription = "Нормально";
            } else {
                ratingColor = Color.rgb(129, 201, 149);
                ratingDescription = "Отлично";
            }

            name.setText(product.getName());

            rating.setText(
                    "Оценка: " + score + "/10 · " + ratingDescription
            );
            rating.setTextColor(ratingColor);

            // Создаём шкалу из десяти делений.
            ratingBar.removeAllViews();

            for (int i = 1; i <= 10; i++) {
                View segment = new View(getContext());

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                0,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                1f
                        );

                params.setMargins(0, 0, i == 10 ? 0 : 3, 0);
                segment.setLayoutParams(params);

                GradientDrawable background = new GradientDrawable();
                background.setCornerRadius(
                        3 * getContext().getResources()
                                .getDisplayMetrics().density
                );

                if (i <= score) {
                    background.setColor(ratingColor);
                } else {
                    background.setColor(Color.rgb(65, 61, 70));
                }

                segment.setBackground(background);
                ratingBar.addView(segment);
            }

            // Заглушка, если у товара нет фотографии.
            thumbnail.setImageResource(R.drawable.no_photo);

            String photoPath = product.getPhotoPath();

            if (photoPath != null && !photoPath.trim().isEmpty()) {
                File photoFile = new File(photoPath);

                if (photoFile.isFile()) {
                    thumbnail.setImageBitmap(
                            BitmapFactory.decodeFile(
                                    photoFile.getAbsolutePath()
                            )
                    );
                }
            }
        }

        return row;
    }
}