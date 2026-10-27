package com.example.myreviews;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.database.sqlite.SQLiteDatabase;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.AlertDialog;
import java.io.File;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> categories;
    private ArrayList<Long> categoryIds;
    private ArrayList<String> categoryIcons;
    private CategoryAdapter adapter;
    private ArrayList<Integer> productCounts;
    private DatabaseHelper databaseHelper;
    private String categorySortOrder = "alphabet";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);

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

        Button btnAddCategory = findViewById(R.id.btnAddCategory);
        ListView listCategories = findViewById(R.id.listCategories);
        Button btnSortCategories = findViewById(R.id.btnSortCategories);

        databaseHelper = new DatabaseHelper(this);

        PhotoCleanupManager.runIfNeeded(this, databaseHelper);
        android.content.SharedPreferences preferences =
                getSharedPreferences("MyReviewsSettings", MODE_PRIVATE);

        categorySortOrder = preferences.getString(
                "category_sort_order", "alphabet"
        );

        categories = new ArrayList<>();
        categoryIds = new ArrayList<>();
        productCounts = new ArrayList<>();
        categoryIcons = new ArrayList<>();

        adapter = new CategoryAdapter(
                this,
                categories,
                productCounts,
                categoryIcons
        );

        listCategories.setAdapter(adapter);

        loadCategories();

        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());


        btnSortCategories.setOnClickListener(v -> {
            String[] options = {
                    "По порядку добавления",
                    "По алфавиту А–Я"
            };

            int selected = categorySortOrder.equals("added") ? 0 : 1;

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Сортировка категорий")
                    .setSingleChoiceItems(options, selected, (dialog, which) -> {

                        categorySortOrder = (which == 0)
                                ? "added"
                                : "alphabet";

                        getSharedPreferences(
                                "MyReviewsSettings", MODE_PRIVATE
                        ).edit()
                                .putString("category_sort_order", categorySortOrder)
                                .apply();

                        loadCategories();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });


        listCategories.setOnItemClickListener((parent, view, position, id) -> {
            String categoryName = categories.get(position);
            long categoryId = categoryIds.get(position);

            Intent intent = new Intent(
                    MainActivity.this,
                    CategoryActivity.class
            );

            intent.putExtra("category_name", categoryName);
            intent.putExtra("category_id", categoryId);

            startActivity(intent);
        });

        listCategories.setOnItemLongClickListener((parent, view, position, id) -> {
            long categoryId = categoryIds.get(position);
            String categoryName = categories.get(position);

            String[] options = {"Переименовать", "Удалить"};

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(categoryName)
                    .setItems(options, (dialog, which) -> {


                        if (which == 0) {
                            EditText input = new EditText(MainActivity.this);
                            input.setText(categoryName);
                            input.setSingleLine(true);
                            input.setSelection(categoryName.length());

                            String[] icons = {
                                    "🛒", "🥤", "🌭", "🍬",
                                    "🧀", "🥐", "☕", "🥛",
                                    "🍫", "🍎", "🍕", "🍦",
                                    "🥩", "🐟", "🥚", "🍞"
                            };

                            int currentIconPosition =
                                    icons.length - 1;

                            String currentIcon = categoryIcons.get(position);

                            for (int i = 0; i < icons.length; i++) {
                                if (icons[i].equals(currentIcon)) {
                                    currentIconPosition = i;
                                    break;
                                }
                            }

                            android.widget.Spinner iconSpinner =
                                    new android.widget.Spinner(MainActivity.this);

                            ArrayAdapter<String> iconAdapter =
                                    new ArrayAdapter<>(
                                            MainActivity.this,
                                            android.R.layout.simple_spinner_item,
                                            icons
                                    );

                            iconAdapter.setDropDownViewResource(
                                    android.R.layout.simple_spinner_dropdown_item
                            );

                            iconSpinner.setAdapter(iconAdapter);
                            iconSpinner.setSelection(currentIconPosition);

                            android.widget.LinearLayout layout =
                                    new android.widget.LinearLayout(MainActivity.this);

                            layout.setOrientation(android.widget.LinearLayout.VERTICAL);

                            int padding = (int) (20 * getResources()
                                    .getDisplayMetrics().density);

                            layout.setPadding(padding, padding / 2, padding, 0);
                            layout.addView(input);
                            layout.addView(iconSpinner);

                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Изменить категорию")
                                    .setView(layout)
                                    .setPositiveButton("Сохранить", (d, w) -> {
                                        String newName =
                                                input.getText().toString().trim();

                                        if (newName.isEmpty()) {
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "Введи название категории",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                            return;
                                        }

                                        String newIcon = icons[
                                                iconSpinner.getSelectedItemPosition()
                                                ];

                                        int result = databaseHelper.updateCategory(
                                                categoryId,
                                                newName,
                                                newIcon
                                        );

                                        if (result == -1) {
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "Такая категория уже существует",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        } else if (result == 0) {
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "Изменения не сохранены",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        } else {
                                            loadCategories();

                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "Категория обновлена",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    })
                                    .setNegativeButton("Отмена", null)
                                    .show();


                    } else if (which == 1) {
                            // Удаление категории
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Удалить категорию?")
                                    .setMessage(
                                            "Категория «" + categoryName +
                                                    "» и все её товары будут удалены. " +
                                                    "Это действие нельзя отменить."
                                    )
                                    .setPositiveButton("Удалить", (d, w) -> {
                                        int result = databaseHelper.deleteCategory(
                                                categoryId
                                        );

                                        if (result > 0) {
                                            loadCategories();
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "Категория удалена",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        } else {
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "Не удалось удалить категорию",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    })
                                    .setNegativeButton("Отмена", null)
                                    .show();
                        }
                    })
                    .setNegativeButton("Отмена", null)
                    .show();

            return true;
        });



    }


    private void loadCategories() {
        categories.clear();
        categoryIds.clear();
        productCounts.clear();
        categoryIcons.clear();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = databaseHelper.getCategories(categorySortOrder)) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(
                        cursor.getColumnIndexOrThrow("id")
                );

                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name")
                );

                String icon = cursor.getString(
                        cursor.getColumnIndexOrThrow("icon")
                );

                categories.add(name);
                categoryIds.add(id);
                categoryIcons.add(icon);

                int count = 0;

                try (Cursor countCursor = db.rawQuery(
                        "SELECT COUNT(*) FROM products WHERE category_id = ?",
                        new String[]{String.valueOf(id)}
                )) {
                    if (countCursor.moveToFirst()) {
                        count = countCursor.getInt(0);
                    }
                }

                productCounts.add(count);
            }
        }

        adapter.notifyDataSetChanged();
    }



    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("Например, Сыры");
        input.setSingleLine(true);

        String[] icons = {
                "🛒", "🥤", "🌭", "🍬",
                "🧀", "🥐", "☕", "🥛",
                "🍫", "🍎", "🍕", "🍦",
                "🥩", "🐟", "🥚", "🍞"
        };

        final String[] selectedIcon = {"🛒"};

        android.widget.Spinner iconSpinner =
                new android.widget.Spinner(this);

        ArrayAdapter<String> iconAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        icons
                );

        iconAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        iconSpinner.setAdapter(iconAdapter);

        android.widget.LinearLayout layout =
                new android.widget.LinearLayout(this);

        layout.setOrientation(android.widget.LinearLayout.VERTICAL);

        int padding = (int) (20 * getResources()
                .getDisplayMetrics().density);

        layout.setPadding(padding, padding / 2, padding, 0);

        layout.addView(input);
        layout.addView(iconSpinner);

        new AlertDialog.Builder(this)
                .setTitle("Новая категория")
                .setView(layout)
                .setPositiveButton("Создать", (dialog, which) -> {
                    String name = input.getText()
                            .toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Введи название категории",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    String icon = icons[
                            iconSpinner.getSelectedItemPosition()
                            ];

                    long result = databaseHelper.addCategory(
                            name, icon
                    );

                    if (result == -1) {
                        Toast.makeText(
                                this,
                                "Такая категория уже есть",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    loadCategories();

                    Toast.makeText(
                            this,
                            "Категория сохранена",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (databaseHelper != null && adapter != null) {
            loadCategories();
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