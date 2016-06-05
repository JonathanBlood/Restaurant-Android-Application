package com.jonathanblood.restaurant.database;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.jonathanblood.restaurant.models.Restaurant;
import com.jonathanblood.restaurant.utils.StringUtils;

import java.util.List;

/**
 * Created by Jonat on 04/06/2016.
 */
public class RestaurantDao
{
    private static final String TAG = "RestaurantDao";
    private FirebaseDatabase database;
    private DatabaseReference rootReference;

    public RestaurantDao()
    {
        database = FirebaseDatabase.getInstance();
        // TODO I need to first configure Picasso to use off line caching.
        //database.setPersistenceEnabled(true);
        rootReference = database.getReference(Restaurant.RESTAURANT_KEY);
    }

    public String save(Restaurant restaurant)
    {
        if (restaurant == null)
        {
            Log.d(TAG, "Missing required parameter");
            return null;
        }

        if (StringUtils.isEmpty(restaurant.getName()))
        {
            Log.w(TAG, "Missing required field: name");
            return null;
        }

        String key = rootReference.push().getKey();
        rootReference.child(key).setValue(restaurant);
        return key;
    }

    public Query getAll()
    {
        return rootReference.orderByChild("rating");
    }

}
