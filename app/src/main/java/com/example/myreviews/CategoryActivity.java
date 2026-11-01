package com.example.myreviews;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import java.io.File;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class CategoryActivity extends AppCompatActivity {
    private String productSortOrder = "alphabet";

    private DatabaseHelper databaseHelper;
    private long categoryId;

    private ArrayList<Product> products;
    private ArrayList<Product> allProducts;
    private ProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        View root = findViewById(R.id.category_main);

        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            view.setPadding(
                    initialLeft + insets.left,
                    initialTop + insets.top,
                    initialRight + insets.right,
                    initialBottom + insets.bottom
            );

            return windowInsets;
        });

        TextView tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        Button btnAddProduct = findViewById(R.id.btnAddProduct);
        GridView listProducts = findViewById(R.id.listProducts);
        EditText etSearchProduct = findViewById(R.id.etSearchProduct);
        Button btnSortProducts = findViewById(R.id.btnSortProducts);

        btnSortProducts.setOnClickListener(v -> {
            String[] options = {
                    "По порядку добавления",
                    "По алфавиту А–Я",
                    "По рейтингу 10–1"
            };

            int selected;

            if ("added".equals(productSortOrder)) {
                selected = 0;
            } else if ("rating".equals(productSortOrder)) {
                selected = 2;
            } else {
                selected = 1;
            }

            new AlertDialog.Builder(CategoryActivity.this)
                    .setTitle("Сортировка товаров")
                    .setSingleChoiceItems(options, selected, (dialog, which) -> {
                        if (which == 0) {
                            productSortOrder = "added";
                        } else if (which == 2) {
                            productSortOrder = "rating";
                        } else {
                            productSortOrder = "alphabet";
                        }

                        getSharedPreferences("MyReviewsSettings", MODE_PRIVATE)
                                .edit()
                                .putString("product_sort_" + categoryId, productSortOrder)
                                .apply();

                        loadProducts();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        String categoryName = getIntent().getStringExtra("category_name");
        categoryId = getIntent().getLongExtra("category_id", -1);
        android.content.SharedPreferences preferences =
                getSharedPreferences("MyReviewsSettings", MODE_PRIVATE);

        productSortOrder = preferences.getString(
                "product_sort_" + categoryId,
                "alphabet"
        );

        if (categoryName != null) {
            tvCategoryTitle.setText(categoryName);
        }

        if (categoryId == -1) {
            Toast.makeText(this, "Категория не найдена",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseHelper = new DatabaseHelper(this);

        allProducts = new ArrayList<>();
        products = new ArrayList<>();

        adapter = new ProductAdapter(this, products);
        listProducts.setAdapter(adapter);

        listProducts.setOnItemClickListener((parent, view, position, id) -> {
            Product product = products.get(position);

            Intent intent = new Intent(
                    CategoryActivity.this,
                    AddProductActivity.class
            );

            intent.putExtra("category_id", categoryId);
            intent.putExtra("product_id", product.getId());

            startActivity(intent);
        });

        etSearchProduct.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());

                if (s.length() > 0) {
                    etSearchProduct.setCompoundDrawablesWithIntrinsicBounds(
                            android.R.drawable.ic_menu_search,
                            0,
                            android.R.drawable.ic_menu_close_clear_cancel,
                            0
                    );
                } else {
                    etSearchProduct.setCompoundDrawablesWithIntrinsicBounds(
                            android.R.drawable.ic_menu_search,
                            0,
                            0,
                            0
                    );
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etSearchProduct.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_menu_search,
                0,
                0,
                0
        );

        etSearchProduct.setOnTouchListener((view, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                android.graphics.drawable.Drawable clearIcon =
                        etSearchProduct.getCompoundDrawables()[2];

                if (clearIcon != null &&
                        event.getX() >= etSearchProduct.getWidth()
                                - etSearchProduct.getPaddingRight()
                                - clearIcon.getBounds().width()) {

                    etSearchProduct.setText("");
                    return true;
                }
            }
            return false;
        });


        btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(
                    CategoryActivity.this,
                    AddProductActivity.class
            );

            intent.putExtra("category_id", categoryId);
            startActivity(intent);
        });


        listProducts.setOnItemLongClickListener(
                (parent, view, position, id) -> {

                    Product product = products.get(position);

                    new AlertDialog.Builder(CategoryActivity.this)
                            .setTitle("Удалить товар?")
                            .setMessage(
                                    "Удалить «" + product.getName() + "»?"
                            )
                            .setPositiveButton("Удалить", (dialog, which) -> {

                                int deleted = databaseHelper.deleteProduct(
                                        product.getId()
                                );

                                if (deleted > 0) {

                                    // Удаляем файл фотографии, если он есть.
                                    String photoPath = product.getPhotoPath();

                                    if (photoPath != null && !photoPath.isEmpty()) {
                                        File photoFile = new File(photoPath);

                                        if (photoFile.exists()) {
                                            photoFile.delete();
                                        }
                                    }

                                    loadProducts();

                                    Toast.makeText(
                                            CategoryActivity.this,
                                            "Товар удалён",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                } else {
                                    Toast.makeText(
                                            CategoryActivity.this,
                                            "Не удалось удалить товар",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            })
                            .setNegativeButton("Отмена", null)
                            .show();

                    return true;
                }
        );


        loadProducts();
    }

    private void loadProducts() {
        allProducts.clear();

        try (Cursor cursor = databaseHelper.getProducts(categoryId, productSortOrder)) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int rating = cursor.getInt(cursor.getColumnIndexOrThrow("rating"));
                String review = cursor.getString(cursor.getColumnIndexOrThrow("review"));
                String photoPath = cursor.getString(cursor.getColumnIndexOrThrow("photo_path"));

                allProducts.add(new Product(id, categoryId, name, rating, review, photoPath));
            }
        }

        EditText etSearchProduct = findViewById(R.id.etSearchProduct);
        filterProducts(etSearchProduct.getText().toString());
    }

    private void filterProducts(String query) {
        products.clear();

        String searchText = query.trim().toLowerCase(Locale.ROOT);

        for (Product product : allProducts) {
            String productName = product.getName().toLowerCase(Locale.ROOT);

            if (productName.contains(searchText)) {
                products.add(product);
            }
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (databaseHelper != null && adapter != null) {
            loadProducts();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}