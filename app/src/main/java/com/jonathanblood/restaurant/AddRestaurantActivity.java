package com.jonathanblood.restaurant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jonathanblood.restaurant.database.RestaurantDao;
import com.jonathanblood.restaurant.models.Restaurant;
import com.jonathanblood.restaurant.utils.StringUtils;

import java.io.File;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddRestaurantActivity extends BaseActivity
{
    private static final String TAG = "AddRestaurantActivity";
    public static final String EXTRA_ADD_RESTAURANT = "AddRestaurantEvent";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private boolean mSaved;
    private boolean mImageSavedToLocalStorage;
    private Uri mFileUri = null;
    private Uri mDownloadUrl;

    private RestaurantDao restaurantDao;
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;


    @BindView(R.id.rating)
    Spinner mSpinner;

    @BindView(R.id.name)
    EditText mNameEditText;

    @BindView(R.id.description)
    EditText mDescriptionEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_restaurant);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_add_restaurant);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mSpinner.setAdapter(ArrayAdapter.createFromResource(this,
                R.array.ratings_array, android.R.layout.simple_spinner_dropdown_item));

        restaurantDao = new RestaurantDao();
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mSaved = false;
        mImageSavedToLocalStorage = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home)
        {
            finish();
            return true;
        } else if (id == R.id.nav_add)
        {
            save();
        } else if (id == R.id.nav_photo)
        {
            dispatchTakePictureIntent();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                if (mFileUri != null) {
                    mImageSavedToLocalStorage = true;
                } else {
                    Log.w(TAG, "File URI is null");
                }
            } else {
                Toast.makeText(this, getString(R.string.picture_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Save restaurant.
     */
    private void save()
    {
        String name = mNameEditText.getText().toString();
        String rating = mSpinner.getSelectedItem().toString();
        String description = mDescriptionEditText.getText().toString();

        if (!validateInput())
            return ;

        FirebaseUser user = mAuth.getCurrentUser();
        String displayName = getString(R.string.unknown);
        if(user != null)
            displayName = user.getDisplayName();
        String photoUri = null;
        if (mFileUri != null)
        {
            uploadFromUri(mFileUri, name, rating, description, displayName);
            return ;

        }

        Restaurant restaurant = new Restaurant(name, rating, description, displayName, null);
        String key = restaurantDao.save(restaurant);

        if (key != null)
            mSaved = true;

        redirectToHome();
    }

    /**
     * Redirect the user back to the home page.
     */
    private void redirectToHome()
    {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_ADD_RESTAURANT, mSaved);
        startActivity(intent);
    }

    /**
     * Validate the input
     * @return true if input is valid, otherwise false
     */
    private boolean validateInput()
    {
        if (StringUtils.isEmpty(mNameEditText.getText().toString()))
        {
            mNameEditText.setError(getString(R.string.required));
            return false;
        }
        return true;
    }

    /**
     * Take picture so it can be stored against restaurant
     */
    private void dispatchTakePictureIntent()
    {
        mImageSavedToLocalStorage = false;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null)
        {
            // Choose file storage location
            File file = new File(Environment.getExternalStorageDirectory(), UUID.randomUUID().toString() + ".jpg");
            mFileUri = Uri.fromFile(file);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * Upload and save photo
     * @param fileUri
     * @param name
     * @param rating
     * @param description
     * @param displayName
     */
    private void uploadFromUri(Uri fileUri, final String name, final String rating, final String description, final String displayName) {
        Log.d(TAG, "uploadFromUri:src:" + fileUri.toString());

        // Get a reference to store file at photos/<FILENAME>.jpg
        final StorageReference photoRef = mStorageRef.child(displayName).child(name)
                .child(fileUri.getLastPathSegment());

        // Upload file to Firebase Storage
        showProgressDialog();

        Log.d(TAG, "uploadFromUri:dst:" + photoRef.getPath());
        photoRef.putFile(fileUri)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Upload succeeded
                        Log.d(TAG, "uploadFromUri:onSuccess");

                        // Get the public download URL
                        mDownloadUrl = taskSnapshot.getDownloadUrl();

                        Restaurant restaurant = new Restaurant(name, rating, description, displayName, mDownloadUrl.toString());
                        String key = restaurantDao.save(restaurant);

                        if (key != null)
                            mSaved = true;

                        redirectToHome();

                        hideProgressDialog();
                        Log.d(TAG, "Photo uploaded to cloud storage: " + mDownloadUrl);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Upload failed
                        Log.w(TAG, "uploadFromUri:onFailure", exception);

                        mDownloadUrl = null;

                        hideProgressDialog();
                        Toast.makeText(AddRestaurantActivity.this, getString(R.string.failed_to_upload_photo), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
