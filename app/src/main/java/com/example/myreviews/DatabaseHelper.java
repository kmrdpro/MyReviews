package com.example.myreviews;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "myreviews.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE categories (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL COLLATE NOCASE UNIQUE)"
        );

        db.execSQL(
                "CREATE TABLE products (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "category_id INTEGER NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "rating INTEGER NOT NULL CHECK(rating BETWEEN 1 AND 10), " +
                        "review TEXT, " +
                        "photo_path TEXT, " +
                        "FOREIGN KEY(category_id) REFERENCES categories(id) " +
                        "ON DELETE CASCADE)"
        );

        db.execSQL("PRAGMA foreign_keys = ON");
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {
        // Миграции структуры базы добавим по мере развития приложения.
    }

    public long addCategory(String name) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name.trim());

        return db.insert("categories", null, values);
    }


    public Cursor getCategories(String sortOrder) {
        SQLiteDatabase db = getReadableDatabase();

        String orderBy;

        if ("added".equals(sortOrder)) {
            orderBy = "id ASC";
        } else {
            orderBy = "name COLLATE NOCASE ASC";
        }

        return db.query(
                "categories",
                new String[]{"id", "name"},
                null,
                null,
                null,
                null,
                orderBy
        );
    }


    public long addProduct(
            long categoryId,
            String name,
            int rating,
            String review,
            String photoPath
    ) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("category_id", categoryId);
        values.put("name", name.trim());
        values.put("rating", rating);
        values.put("review", review);
        values.put("photo_path", photoPath);

        return db.insert("products", null, values);
    }


    public int deleteProduct(long productId) {
        SQLiteDatabase db = getWritableDatabase();

        return db.delete(
                "products",
                "id = ?",
                new String[]{String.valueOf(productId)}
        );
    }


    public int deleteCategory(long categoryId) {
        SQLiteDatabase db = getWritableDatabase();

        return db.delete(
                "categories",
                "id = ?",
                new String[]{String.valueOf(categoryId)}
        );
    }




    public Cursor getProducts(long categoryId, String sortOrder) {
        SQLiteDatabase db = getReadableDatabase();

        String orderBy;

        if ("added".equals(sortOrder)) {
            // Сначала добавленные товары
            orderBy = "id ASC";

        } else if ("rating".equals(sortOrder)) {
            // Сначала товары с высоким рейтингом
            orderBy = "rating DESC, name COLLATE NOCASE ASC";

        } else {
            // По умолчанию — по названию А–Я
            orderBy = "name COLLATE NOCASE ASC";
        }

        return db.query(
                "products",
                new String[]{"id", "name", "rating", "review", "photo_path"},
                "category_id = ?",
                new String[]{String.valueOf(categoryId)},
                null,
                null,
                orderBy
        );
    }



    public Cursor getProduct(long productId) {
        SQLiteDatabase db = getReadableDatabase();

        return db.query(
                "products",
                new String[]{
                        "id",
                        "category_id",
                        "name",
                        "rating",
                        "review",
                        "photo_path"
                },
                "id = ?",
                new String[]{String.valueOf(productId)},
                null,
                null,
                null
        );
    }

    public int updateProduct(
            long productId,
            String name,
            int rating,
            String review,
            String photoPath
    ) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        values.put("rating", rating);
        values.put("review", review);
        values.put("photo_path", photoPath);

        return db.update(
                "products",
                values,
                "id = ?",
                new String[]{String.valueOf(productId)}
        );
    }

    public int renameCategory(long categoryId, String newName) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", newName.trim());

        return db.update(
                "categories",
                values,
                "id = ?",
                new String[]{String.valueOf(categoryId)}
        );
    }

}