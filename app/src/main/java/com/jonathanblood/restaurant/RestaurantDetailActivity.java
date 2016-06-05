package com.jonathanblood.restaurant;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatRatingBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.jonathanblood.restaurant.database.RestaurantDao;
import com.jonathanblood.restaurant.models.Restaurant;
import com.jonathanblood.restaurant.picasso.CircleTransformation;
import com.jonathanblood.restaurant.utils.StringUtils;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RestaurantDetailActivity extends BaseActivity
{
    private static final String TAG = "ResDetailActivity";
    public static final String EXTRA_RESTAURANT_KEY = "Key";
    public static final String EXTRA_RESTAURANT_NAME = "Name";
    public static final String EXTRA_DELETE_EVENT = "Delete";

    private String mKey = null;
    private String mName = null;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRestaurantReference;
    private ValueEventListener mRestaurantListener;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.fab)
    FloatingActionButton mEditButton;

    @BindView(R.id.description)
    TextView mDescriptionTextView;

    @BindView(R.id.picture)
    ImageView mRestaurantImageView;

    @BindView(R.id.rating)
    TextView mRating;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_detail);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

        // Get key
        mKey = getIntent().getStringExtra(EXTRA_RESTAURANT_KEY);
        mName = getIntent().getStringExtra(EXTRA_RESTAURANT_NAME);

        if (mKey == null)
            throw new IllegalArgumentException("Must pass EXTRA_RESTAURANT_KEY");

        // Set title
        if (getSupportActionBar() != null )
            getSupportActionBar().setTitle(mName);

        // Initialize database
        mDatabase = FirebaseDatabase.getInstance();
        mRestaurantReference = mDatabase.getReference(Restaurant.RESTAURANT_KEY).child(mKey);

        loadData();

    }

    @Override
    public void onStop()
    {
        super.onStop();

        // Remove post value event listener
        if (mRestaurantListener != null)
        {
            mRestaurantReference.removeEventListener(mRestaurantListener);
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();

    }

    /**
     * Load data from the firebase database and set it to the view.
     */
    private void loadData()
    {
        ValueEventListener restaurantListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showProgressDialog();
                // Get Post object and use the values to update the UI
                Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);
                if (restaurant == null)
                {
                    hideProgressDialog();
                    return ;
                }

                String pictureUri = restaurant.getPhotoUri();

                // Set Description
                mDescriptionTextView.setText(restaurant.getDescription());

                // Set rating
                String rating = restaurant.getRating();
                if (StringUtils.isEmpty(rating) || rating.equals(getString(R.string.none)))
                    mRating.setText("-");
                else
                    mRating.setText(rating);

                // Set picture
                if(pictureUri != null)
                    setRestaurantImage(pictureUri);
                else
                    hideProgressDialog();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Restaurant failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(RestaurantDetailActivity.this, "Failed to load restaurant.", Toast.LENGTH_SHORT).show();
            }
        };
        mRestaurantReference.addValueEventListener(restaurantListener);
        // [END post_value_event_listener]

        // Keep copy of post listener so we can remove it when app stops
        mRestaurantListener = restaurantListener;
    }

    /**
     * Set the restaurant image.
     * @param pictureUri
     */
    private void setRestaurantImage(String pictureUri)
    {
        Picasso.with(RestaurantDetailActivity.this).load(pictureUri).resize(mRestaurantImageView.getWidth(), mRestaurantImageView.getHeight()).into(mRestaurantImageView, new com.squareup.picasso.Callback() {
            @Override
            public void onSuccess() {
                hideProgressDialog();
            }

            @Override
            public void onError() {
                hideProgressDialog();
            }
        });
    }

    @OnClick(R.id.fab)
    public void onEdit(View view)
    {
        Log.d(TAG, "onEdit");
        mRestaurantReference.removeValue();
        Intent intent = new Intent(RestaurantDetailActivity.this, MainActivity.class);
        intent.putExtra(EXTRA_DELETE_EVENT, true);
        startActivity(intent);
    }
}
