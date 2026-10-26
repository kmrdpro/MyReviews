
package com.example.myreviews;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AddProductActivity extends AppCompatActivity {

    private EditText etProductName;
    private EditText etRating;
    private EditText etReview;
    private ImageView ivProductPhoto;
    private Button btnSaveProduct;

    private DatabaseHelper databaseHelper;

    private long categoryId;
    private long productId = -1;

    private String photoPath;
    private String originalPhotoPath;

    private ActivityResultLauncher<String> photoPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        categoryId = getIntent().getLongExtra("category_id", -1);
        productId = getIntent().getLongExtra("product_id", -1);

        if (categoryId == -1) {
            Toast.makeText(this, "Категория не найдена",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseHelper = new DatabaseHelper(this);

        etProductName = findViewById(R.id.etProductName);
        etRating = findViewById(R.id.etRating);
        etReview = findViewById(R.id.etReview);
        ivProductPhoto = findViewById(R.id.ivProductPhoto);

        Button btnChoosePhoto = findViewById(R.id.btnChoosePhoto);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);

        photoPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String newPath = copyPhotoToStorage(uri);

                        if (newPath != null) {
                            photoPath = newPath;
                            ivProductPhoto.setImageURI(Uri.fromFile(
                                    new File(photoPath)
                            ));
                        }
                    }
                }
        );

        btnChoosePhoto.setOnClickListener(v ->
                photoPicker.launch("image/*"));

        btnSaveProduct.setOnClickListener(v -> saveProduct());

        if (productId != -1) {
            setTitle("Карточка товара");
            btnSaveProduct.setText("Сохранить изменения");
            loadProduct();
        } else {
            setTitle("Новый товар");
        }
    }

    private void loadProduct() {
        try (Cursor cursor = databaseHelper.getProduct(productId)) {
            if (cursor.moveToFirst()) {
                categoryId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("category_id")
                );

                etProductName.setText(cursor.getString(
                        cursor.getColumnIndexOrThrow("name")
                ));

                etRating.setText(String.valueOf(cursor.getInt(
                        cursor.getColumnIndexOrThrow("rating")
                )));

                etReview.setText(cursor.getString(
                        cursor.getColumnIndexOrThrow("review")
                ));

                photoPath = cursor.getString(
                        cursor.getColumnIndexOrThrow("photo_path")
                );

                originalPhotoPath = photoPath;

                if (photoPath != null && !photoPath.isEmpty()) {
                    File photoFile = new File(photoPath);

                    if (photoFile.exists()) {
                        ivProductPhoto.setImageURI(
                                Uri.fromFile(photoFile)
                        );
                    }
                }
            } else {
                Toast.makeText(this, "Товар не найден",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String copyPhotoToStorage(Uri uri) {
        File imageDirectory = new File(getFilesDir(), "images");

        if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
            Toast.makeText(this, "Не удалось создать папку для фото",
                    Toast.LENGTH_SHORT).show();
            return null;
        }

        File imageFile = new File(
                imageDirectory,
                "product_" + System.currentTimeMillis() + ".jpg"
        );

        try (InputStream input =
                     getContentResolver().openInputStream(uri);
             FileOutputStream output =
                     new FileOutputStream(imageFile)) {

            if (input == null) {
                return null;
            }

            byte[] buffer = new byte[8192];
            int length;

            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }

            return imageFile.getAbsolutePath();

        } catch (Exception e) {
            Toast.makeText(this, "Не удалось сохранить фотографию",
                    Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void saveProduct() {
        String name = etProductName.getText().toString().trim();
        String ratingText = etRating.getText().toString().trim();
        String review = etReview.getText().toString().trim();

        if (name.isEmpty()) {
            etProductName.setError("Введи название товара");
            return;
        }

        if (ratingText.isEmpty()) {
            etRating.setError("Введи оценку от 1 до 10");
            return;
        }

        int rating;

        try {
            rating = Integer.parseInt(ratingText);
        } catch (NumberFormatException e) {
            etRating.setError("Введи целое число от 1 до 10");
            return;
        }

        if (rating < 1 || rating > 10) {
            etRating.setError("Оценка должна быть от 1 до 10");
            return;
        }

        long result;

        if (productId == -1) {
            result = databaseHelper.addProduct(
                    categoryId,
                    name,
                    rating,
                    review,
                    photoPath
            );
        } else {
            result = databaseHelper.updateProduct(
                    productId,
                    name,
                    rating,
                    review,
                    photoPath
            );
        }

        if (result == -1 || result == 0) {
            Toast.makeText(this, "Не удалось сохранить товар",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Старое фото удаляем только после успешного сохранения.
        if (originalPhotoPath != null
                && !originalPhotoPath.equals(photoPath)) {
            File oldPhoto = new File(originalPhotoPath);
            if (oldPhoto.exists()) {
                oldPhoto.delete();
            }
        }

        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
