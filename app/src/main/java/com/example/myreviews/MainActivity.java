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

import android.app.AlertDialog;
import java.io.File;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> categories;
    private ArrayList<Long> categoryIds;
    private ArrayAdapter<String> adapter;
    private DatabaseHelper databaseHelper;
    private String categorySortOrder = "alphabet";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAddCategory = findViewById(R.id.btnAddCategory);
        ListView listCategories = findViewById(R.id.listCategories);
        Button btnSortCategories = findViewById(R.id.btnSortCategories);

        databaseHelper = new DatabaseHelper(this);
        android.content.SharedPreferences preferences =
                getSharedPreferences("MyReviewsSettings", MODE_PRIVATE);

        categorySortOrder = preferences.getString(
                "category_sort_order", "alphabet"
        );

        categories = new ArrayList<>();
        categoryIds = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                categories
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
                            // Переименование категории
                            EditText input = new EditText(MainActivity.this);
                            input.setText(categoryName);
                            input.setSingleLine(true);
                            input.setSelection(categoryName.length());

                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Переименовать категорию")
                                    .setView(input)
                                    .setPositiveButton("Сохранить", (d, w) -> {

                                        String newName = input.getText().toString().trim();


                                        if (newName.isEmpty()) {
                                            return;
                                        }

                                        if (newName.equalsIgnoreCase(categoryName)) {
                                            return;
                                        }

                                        long result = databaseHelper.renameCategory(
                                                categoryId, newName
                                        );
                                        Toast.makeText(
                                                MainActivity.this,
                                                "ID категории: " + categoryId + ", результат SQLite: " + result,
                                                Toast.LENGTH_LONG
                                        ).show();

                                        if (result == -1) {
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "Такая категория уже существует",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        } else if (result == 0) {
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "Не удалось переименовать категорию",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        } else {
                                            loadCategories();
                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "Категория переименована",
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

        try (Cursor cursor = databaseHelper.getCategories(categorySortOrder)) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(
                        cursor.getColumnIndexOrThrow("id")
                );

                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name")
                );

                categoryIds.add(id);
                categories.add(name);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("Например, Квасы");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("Новая категория")
                .setView(input)
                .setPositiveButton("Создать", (dialog, which) -> {
                    String name = input.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Введи название категории",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    long result = databaseHelper.addCategory(name);

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