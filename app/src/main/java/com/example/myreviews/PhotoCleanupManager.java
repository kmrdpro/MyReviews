package com.example.myreviews;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoCleanupManager {

    private static final String PREFS_NAME = "photo_cleanup";
    private static final String LAST_CLEANUP_KEY = "last_cleanup";
    private static final long ONE_DAY = 24L * 60 * 60 * 1000;

    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public static void runIfNeeded(
            Context context,
            DatabaseHelper databaseHelper
    ) {
        Context appContext = context.getApplicationContext();

        SharedPreferences prefs = appContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        long lastCleanup = prefs.getLong(LAST_CLEANUP_KEY, 0);
        long now = System.currentTimeMillis();

        if (now - lastCleanup < ONE_DAY) {
            return;
        }

        executor.execute(() -> {
            try {
                boolean success = cleanup(appContext, databaseHelper);

                if (success) {
                    prefs.edit()
                            .putLong(
                                    LAST_CLEANUP_KEY,
                                    System.currentTimeMillis()
                            )
                            .apply();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static boolean cleanup(
            Context context,
            DatabaseHelper databaseHelper
    ) {
        File imagesDir = new File(context.getFilesDir(), "images");

        // Если папки нет, удалять нечего.
        if (!imagesDir.exists()) {
            return true;
        }

        // Если это не папка или её нельзя прочитать,
        // не считаем очистку успешной.
        if (!imagesDir.isDirectory()) {
            return false;
        }

        File[] files = imagesDir.listFiles();

        if (files == null) {
            return false;
        }

        Set<String> usedPaths = new HashSet<>();

        // Собираем пути всех фотографий, используемых товарами.
        try (Cursor cursor = databaseHelper.getAllPhotoPaths()) {
            int columnIndex = cursor.getColumnIndexOrThrow("photo_path");

            while (cursor.moveToNext()) {
                String path = cursor.getString(columnIndex);

                if (path != null && !path.isEmpty()) {
                    try {
                        usedPaths.add(
                                new File(path).getCanonicalPath()
                        );
                    } catch (IOException e) {
                        // Если путь не удалось проверить,
                        // прекращаем очистку для безопасности.
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }

        // Удаляем только файлы из папки images,
        // которых нет среди путей из базы данных.
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            try {
                String filePath = file.getCanonicalPath();

                if (!usedPaths.contains(filePath)) {
                    if (!file.delete()) {
                        return false;
                    }
                }
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }
}