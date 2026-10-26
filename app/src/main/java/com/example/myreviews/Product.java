package com.example.myreviews;

public class Product {

    private final long id;
    private final long categoryId;
    private final String name;
    private final int rating;
    private final String review;
    private final String photoPath;

    public Product(
            long id,
            long categoryId,
            String name,
            int rating,
            String review,
            String photoPath
    ) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.rating = rating;
        this.review = review;
        this.photoPath = photoPath;
    }

    public long getId() {
        return id;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public int getRating() {
        return rating;
    }

    public String getReview() {
        return review;
    }

    public String getPhotoPath() {
        return photoPath;
    }
}