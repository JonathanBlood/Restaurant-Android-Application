package com.jonathanblood.restaurant.models;

public class Restaurant
{
    public static final String RESTAURANT_KEY  = "restaurants";
    private String name;
    private String rating;
    private String description;
    private String createdBy;
    private String photoUri;

    public Restaurant(){}

    public Restaurant(String name, String rating, String description, String createdBy, String photoUri)
    {
        this.name = name;
        this.rating = rating;
        this.description = description;
        this.createdBy = createdBy;
        this.photoUri = photoUri;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getRating()
    {
        return rating;
    }

    public void setRating(String rating)
    {
        this.rating = rating;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getCreatedBy()
    {
        return createdBy;
    }

    public void setCreatedBy(String createdBy)
    {
        this.createdBy = createdBy;
    }

    public String getPhotoUri()
    {
        return photoUri;
    }

    public void setPhotoUri(String photoUri)
    {
        this.photoUri = photoUri;
    }
}
