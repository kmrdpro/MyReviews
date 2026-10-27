
package com.example.myreviews;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.Dialog;
import android.graphics.Color;
import android.view.Window;
import android.view.WindowManager;

import com.github.chrisbanes.photoview.PhotoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

        android.widget.ScrollView scrollView =
                findViewById(R.id.scrollViewProduct);

        final int initialScrollLeft = scrollView.getPaddingLeft();
        final int initialScrollTop = scrollView.getPaddingTop();
        final int initialScrollRight = scrollView.getPaddingRight();
        final int initialScrollBottom = scrollView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            Insets ime = windowInsets.getInsets(
                    WindowInsetsCompat.Type.ime()
            );

            view.setPadding(
                    initialScrollLeft + systemBars.left,
                    initialScrollTop + systemBars.top,
                    initialScrollRight + systemBars.right,
                    initialScrollBottom + Math.max(systemBars.bottom, ime.bottom)
            );

            return windowInsets;
        });


//        View root = findViewById(R.id.product_main);

//        int initialLeft = root.getPaddingLeft();
//        int initialTop = root.getPaddingTop();
//        int initialRight = root.getPaddingRight();
//        int initialBottom = root.getPaddingBottom();

//        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
//            Insets insets = windowInsets.getInsets(
//                    WindowInsetsCompat.Type.systemBars()
//            );
//
//            view.setPadding(
//                    initialLeft + insets.left,
//                    initialTop + insets.top,
//                    initialRight + insets.right,
//                    initialBottom + insets.bottom
//            );
//
//            return windowInsets;
//        });

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



        final View rootView = findViewById(android.R.id.content);

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            android.graphics.Rect visibleRect =
                    new android.graphics.Rect();

            rootView.getWindowVisibleDisplayFrame(visibleRect);

            int screenHeight = rootView.getRootView().getHeight();
            int visibleHeight = visibleRect.height();

            boolean keyboardVisible =
                    screenHeight - visibleHeight > screenHeight * 0.20;

            if (keyboardVisible && etReview.hasFocus()) {
                rootView.postDelayed(() -> {
                    android.graphics.Rect fieldRect =
                            new android.graphics.Rect();

                    etReview.getGlobalVisibleRect(fieldRect);

                    if (fieldRect.bottom > visibleRect.bottom
                            || fieldRect.top > visibleRect.bottom) {
                        scrollView.smoothScrollBy(
                                0,
                                etReview.getHeight() + 40
                        );
                    }
                }, 100);
            }
        });


        ivProductPhoto.setOnClickListener(v -> showPhotoFullScreen());

        Button btnChoosePhoto = findViewById(R.id.btnChoosePhoto);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);

        Button btnDeleteProduct = findViewById(R.id.btnDeleteProduct);

        if (productId == -1) {
            // Создаём новый товар — удалять пока нечего.
            btnDeleteProduct.setVisibility(View.GONE);
        } else {
            // Редактируем существующий товар.
            btnDeleteProduct.setVisibility(View.VISIBLE);

            btnDeleteProduct.setOnClickListener(v -> confirmDeleteProduct());
        }

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

                if (photoPath != null && !photoPath.trim().isEmpty()) {
                    File photoFile = new File(photoPath);

                    if (photoFile.isFile()) {
                        ivProductPhoto.setImageURI(Uri.fromFile(photoFile));
                    } else {
                        ivProductPhoto.setImageResource(
                                R.drawable.ic_photo_placeholder
                        );
                    }
                } else {
                    ivProductPhoto.setImageResource(
                            R.drawable.ic_photo_placeholder
                    );
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


    private void confirmDeleteProduct() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить товар?")
                .setMessage("Товар и его фотография будут удалены.")
                .setPositiveButton("Удалить", (dialog, which) -> {

                    // 1. Сначала узнаём путь к фотографии.
                    String savedPhotoPath = null;

                    Cursor cursor = databaseHelper.getReadableDatabase().query(
                            "products",
                            new String[]{"photo_path"},
                            "id = ?",
                            new String[]{String.valueOf(productId)},
                            null,
                            null,
                            null
                    );

                    try {
                        if (cursor.moveToFirst()) {
                            savedPhotoPath = cursor.getString(
                                    cursor.getColumnIndexOrThrow("photo_path")
                            );
                        }
                    } finally {
                        cursor.close();
                    }

                    // 2. Удаляем товар из SQLite.
                    int deleted = databaseHelper.getWritableDatabase().delete(
                            "products",
                            "id = ?",
                            new String[]{String.valueOf(productId)}
                    );

                    if (deleted > 0) {

                        // 3. Удаляем файл фотографии, если он существует.
                        if (savedPhotoPath != null
                                && !savedPhotoPath.trim().isEmpty()) {

                            File photoFile = new File(savedPhotoPath);

                            if (photoFile.isFile() && !photoFile.delete()) {
                                Toast.makeText(
                                        this,
                                        "Товар удалён, но фотографию удалить не удалось",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }

                        // 4. Возвращаемся к списку товаров.
                        if (!isFinishing()) {
                            Toast.makeText(
                                    this,
                                    "Товар удалён",
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();
                        }

                    } else {
                        Toast.makeText(
                                this,
                                "Не удалось удалить товар",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .setNegativeButton("Нет", (dialog, which) -> dialog.dismiss())
                .show();
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

    private void showPhotoFullScreen() {
        if (photoPath == null || photoPath.isEmpty()) {
            Toast.makeText(
                    this,
                    "Фотография ещё не выбрана",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        File photoFile = new File(photoPath);

        if (!photoFile.exists()) {
            Toast.makeText(
                    this,
                    "Файл фотографии не найден",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Dialog dialog = new Dialog(
                this,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen
        );

        PhotoView photoView = new PhotoView(this);
        photoView.setBackgroundColor(Color.BLACK);
        photoView.setImageURI(Uri.fromFile(photoFile));

        dialog.setContentView(photoView);
        dialog.show();

        Window window = dialog.getWindow();

        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );
            window.setBackgroundDrawableResource(android.R.color.black);
        }
    }
}
