
package com.example.myreviews;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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

        Product product = getItem(position);

        if (product != null) {
            int score = product.getRating();

            if (score >= 1 && score <= 4) {
                row.setBackgroundColor(
                        android.graphics.Color.rgb(248, 215, 218));
            } else if (score >= 5 && score <= 7) {
                row.setBackgroundColor(
                        android.graphics.Color.rgb(255, 243, 205));
            } else if (score >= 8 && score <= 10) {
                row.setBackgroundColor(
                        android.graphics.Color.rgb(209, 231, 221));
            }

            name.setText(product.getName());
            rating.setText("Оценка: " + product.getRating() + "/10");

            // По умолчанию показываем заглушку.
            thumbnail.setImageResource(R.drawable.no_photo);

            String photoPath = product.getPhotoPath();

            if (photoPath != null && !photoPath.trim().isEmpty()) {
                File photoFile = new File(photoPath);

                if (photoFile.isFile()) {
                    thumbnail.setImageBitmap(
                            BitmapFactory.decodeFile(photoFile.getAbsolutePath())
                    );
                }
            }
        }

        return row;
    }
}
