package com.jonathanblood.restaurant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.jonathanblood.restaurant.database.RestaurantDao;
import com.jonathanblood.restaurant.models.Restaurant;
import com.jonathanblood.restaurant.picasso.CircleTransformation;
import com.jonathanblood.restaurant.utils.StringUtils;
import com.jonathanblood.restaurant.viewholder.RestaurantViewHolder;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener
{

    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 9001;

    /**
     * Google sign in
     */
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private GoogleSignInOptions mGoogleSignInOptions;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseUser user;

    private MenuItem mAuthenticateMenuItem;
    private ImageView mHeaderLogo;
    private TextView mHeaderTitle;
    private TextView mHeaderSubTitle;
    private RecyclerView mRecycler;
    private FirebaseRecyclerAdapter<Restaurant, RestaurantViewHolder> mAdapter;
    private LinearLayoutManager mManager;
    private RestaurantDao restaurantDao;

    @BindView(R.id.fab)
    FloatingActionButton mAddButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initialize();

        Intent intent =  getIntent();

        handleSaveEventSnackBar(intent);
        handleDeleteEventSnackBar(intent);

        restaurantDao = new RestaurantDao();

        // use a linear layout manager
        mRecycler = (RecyclerView) findViewById(R.id.restaurant_list);
        mManager = new LinearLayoutManager(this);
        mManager.setReverseLayout(true);
        mManager.setStackFromEnd(true);
        mRecycler.setLayoutManager(mManager);

        // Set up FirebaseRecyclerAdapter with the Query
        Query restaurantQuery = restaurantDao.getAll();
        createRestaurantRecyclerAdapter(restaurantQuery);

        // Google sign in
        configureGoogleSignIn();
        configureGoogleApiClient();
        createAuthListener();

    }

    @Override
    public void onStart()
    {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (mAuthListener != null)
        {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @OnClick(R.id.fab)
    public void addOnClick(View view)
    {
        Log.d(TAG, "Add on click");
        Intent addIntent = new Intent(this, AddRestaurantActivity.class);
        startActivity(addIntent);
    }

    /**
     * Perform sign in.
     */
    private void signIn()
    {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * Perform sign out.
     */
    private void signOut()
    {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>()
                {
                    @Override
                    public void onResult(@NonNull Status status)
                    {
                        updateUI();
                    }
                });
    }

    /**
     * Handle delete event notification
     * @param intent
     */
    private void handleDeleteEventSnackBar(Intent intent)
    {
        boolean deleteEvent = intent.getBooleanExtra(RestaurantDetailActivity.EXTRA_DELETE_EVENT, false);
        if (deleteEvent)
            Snackbar.make(mAddButton, getString(R.string.restaurant_deleted), Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    /**
     * Handle save event notification
     * @param intent
     */
    private void handleSaveEventSnackBar(Intent intent)
    {
        boolean saveEventOccurred = intent.getBooleanExtra(AddRestaurantActivity.EXTRA_ADD_RESTAURANT, false);
        if (saveEventOccurred)
            Snackbar.make(mAddButton, getString(R.string.restaurant_saved), Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN)
        {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess())
            {
                Log.d(TAG, "Authenticated");
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else
            {
                // Google Sign In failed, update UI appropriately
                Log.d(TAG, "Failed to authenticate");
                updateUI();
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, getString(R.string.google_play_service_error), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        } else
        {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

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
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_authenticate)
        {
            Log.d(TAG, "Authenticate button clicked!");
            if (user != null)
                signOut();
            else
                signIn();
        }

        return true;
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct)
    {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful())
                        {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Configure google sign in options
     *
     * @return Google sign in options
     */
    private void configureGoogleSignIn()
    {
        // Configure Google Sign In
        mGoogleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
    }

    /**
     * Configure the google api client
     */
    private void configureGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGoogleSignInOptions)
                .build();
    }

    /**
     * Creates an authentication listener to detect when a user signs in or out.
     */
    private void createAuthListener()
    {
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener()
        {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)
            {
                user = firebaseAuth.getCurrentUser();
                if (user != null)
                {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else
                {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                updateUI();
            }
        };
    }

    /**
     * Update user interface after authentication changes.
     */
    private void updateUI()
    {
        Log.d(TAG, "Upating User Interface");
        if (user != null)
        {
            Uri photoUri = user.getPhotoUrl();
            String displayName = user.getDisplayName();
            String title = (StringUtils.isEmpty(displayName) ? getString(R.string.app_name) : displayName);
            String email = user.getEmail();

            mAddButton.setVisibility(View.VISIBLE);
            mAuthenticateMenuItem.setTitle(getString(R.string.logout));
            mHeaderTitle.setText(title);
            mHeaderSubTitle.setText(email);
            if (photoUri != null)
            {
                mAuthenticateMenuItem.setIcon(R.drawable.logout_logo);
                Picasso.with(this).load(photoUri).resize(150,150).transform(new CircleTransformation()).into(mHeaderLogo);
            }
        } else
        {
            mAddButton.setVisibility(View.GONE);
            mHeaderTitle.setText(getString(R.string.app_name));
            mHeaderSubTitle.setText(getString(R.string.developer_info));
            mAuthenticateMenuItem.setTitle(getString(R.string.login));
            mAuthenticateMenuItem.setIcon(R.drawable.login_logo);
            mHeaderLogo.setImageResource(R.drawable.food_logo);
        }
    }

    /**
     * Launch Restaurant Detail activity
     * @param key
     * @param name
     */
    private void launchRestaurantDetailActivity(String key, String name)
    {
        Intent intent = new Intent(MainActivity.this, RestaurantDetailActivity.class);
        intent.putExtra(RestaurantDetailActivity.EXTRA_RESTAURANT_NAME, name);
        intent.putExtra(RestaurantDetailActivity.EXTRA_RESTAURANT_KEY, key);
        startActivity(intent);
    }

    /**
     * Create Restaurant recycler adapter.
     * @param restaurantQuery
     */
    private void createRestaurantRecyclerAdapter(final Query restaurantQuery)
    {
        showProgressDialog();
        mAdapter = new FirebaseRecyclerAdapter<Restaurant, RestaurantViewHolder>(Restaurant.class, R.layout.item_restaurant,
                RestaurantViewHolder.class, restaurantQuery) {
            @Override
            protected void populateViewHolder(final RestaurantViewHolder viewHolder, final Restaurant model, final int position) {
                final DatabaseReference restaurantRef = getRef(position);

                // Set click listener for the whole post view
                final String restaurantKey = restaurantRef.getKey();
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchRestaurantDetailActivity(restaurantKey, model.getName());
                    }
                });

                // Bind Post to ViewHolder, setting OnClickListener for the star button
                viewHolder.bindToRestaurant(MainActivity.this, model, getString(R.string.added_by), getString(R.string.none));
                String uri = model.getPhotoUri();
                setRestaurantItemPicture(viewHolder, uri);
            }
        };
        mRecycler.setAdapter(mAdapter);
    }

    /**
     * Set restaurant item picture.
     * @param viewHolder
     * @param uri
     */
    private void setRestaurantItemPicture(RestaurantViewHolder viewHolder, String uri)
    {
        if(uri != null)
        {
            Picasso.with(MainActivity.this).load(uri).resize(120,120).transform(new CircleTransformation()).into(viewHolder.pictureImageView, new com.squareup.picasso.Callback() {
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
    }

    /**
     * Initialize
     */
    private void initialize()
    {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View hView =  navigationView.getHeaderView(0);
        Menu menu = navigationView.getMenu();
        navigationView.setNavigationItemSelectedListener(this);

        mAuthenticateMenuItem = menu.findItem(R.id.nav_authenticate);
        mHeaderLogo = (ImageView) hView.findViewById(R.id.header_logo);
        mHeaderTitle = (TextView) hView.findViewById(R.id.header_title);
        mHeaderSubTitle = (TextView) hView.findViewById(R.id.header_sub_title);
    }
}
