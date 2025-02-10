package com.alsolutions.mapia;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alsolutions.mapia.model.LanguageList;
import com.alsolutions.mapia.model.MarkerInfo;
import com.alsolutions.mapia.model.Markers;
import com.alsolutions.mapia.model.VolleySingleton;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import static com.alsolutions.mapia.R.id.map;

public class MapsMainActivity extends AppCompatActivity implements OnMapReadyCallback
{
    //constants
    private final String TAG = "MapsMainActivity";
    private final int CAMERA_MOVE_REACT_THRESHOLD_MS = 200;
    private final int DOWNLOAD_LIMIT = 250;
    private final int ANIMATE_CAMERA_SPEED_MS = 200;
    private final int MAX_WIKIPEDIA_API_RANGE = 10000;
    private final int MIN_WIKIPEDIA_API_RANGE = 10;
    private long lastCallMs = Long.MIN_VALUE;

    //retained variables
    private GoogleMap mMap;
    private ClusterManager<MarkerInfo> mClusterManager;
    private Markers mMarkers;
    private MarkerInfo mSelectedMarker;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastGPSLocation;

    //activity variables
    private Activity mActivity;
    private ImageView mThumbnailImage;
    private ImageView mLargeImage;
    private View mInfoViewLayout;
    private View mGalleryViewLayout;
    private Toolbar mToolbar;
    private ProgressBar mProgressBar;
    private Animation animShow, animHide;
    private BottomSheetBehavior mBehavior;
    private Circle mCircle;
    private CircleOptions mCircleOptions;
    private CoordinatorLayout mCoordinatorLayout;
    private int mTopPadding, mBottomPadding, mBottomPaddingExpanded;
    private TextToSpeech mTextToSpeech;

    //preferences & settings
    private CameraPosition mLastCameraPosition;
    private String mLanguageCode;
    private int mMapType;
    private boolean bDisplayFavorites;
    private boolean bDisplayMarkers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_main);

        initVariables();
        initConnection();
        loadPreferences();
        initMap();
    }

    private boolean checkLibraries() {
        /*GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE).show();
            }
            return false;
        }*/
        return true;
    }

    private void initVariables(){
        Log.d(TAG,"start of initVariables");
        mActivity = this;

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        //toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mTextToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        });

        //screen density
        int density = (int) (Resources.getSystem().getDisplayMetrics().density);
        mTopPadding = 70*density; //toolbar height
        mBottomPadding = 200*density; //bottomsheet height
        mBottomPaddingExpanded = 520*density;

        //bottomInfoWindow
        //mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.parent_frame_layout);
        //mInfoViewLayout = mCoordinatorLayout.findViewById(R.id.bottom_info_nested_view);
        View bottomSheet = findViewById(R.id.bottom_sheet);
        mBehavior = BottomSheetBehavior.from(bottomSheet);
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        mBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        mMap.setPadding(0, mTopPadding, 0, 0);
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        mMap.setPadding(0, mTopPadding, 0, mBottomPadding);
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        mMap.setPadding(0, mTopPadding, 0, mBottomPaddingExpanded);
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        TextView tv_extract = (TextView) findViewById(R.id.tv_extract);
        tv_extract.setMovementMethod(new ScrollingMovementMethod());

        mThumbnailImage = (ImageView)findViewById(R.id.iv_thumbnail);
        mThumbnailImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) { openGallery(); }
        });
        ImageView addToFavorites = (ImageView) findViewById(R.id.update_bookmark);
        addToFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFavorites();
            }
        });
        TextView title =(TextView)findViewById(R.id.tv_title);
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebView();
            }
        });
        ImageView shareArticle =(ImageView)findViewById(R.id.share);
        shareArticle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, "Sharing Article");
                i.putExtra(Intent.EXTRA_TEXT, "Check out this article\n\n" + mSelectedMarker.getTitle());
                startActivity(Intent.createChooser(i, "Share this article with..."));
            }
        });
        ImageView showOnMap =(ImageView)findViewById(R.id.show_on_map);
        showOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.animateCamera(CameraUpdateFactory.newLatLng(mSelectedMarker.getPosition()),
                        ANIMATE_CAMERA_SPEED_MS, null);
            }
        });
        ImageView hear =(ImageView)findViewById(R.id.hear);
        hear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTextToSpeech.setLanguage(new Locale("en-us"));
                TextView text = (TextView) findViewById(R.id.tv_extract);
                mTextToSpeech.speak(text.getText().toString(),
                        TextToSpeech.QUEUE_FLUSH, null, mSelectedMarker.getPageID());
            }
        });

        //gallery
        mGalleryViewLayout = findViewById(R.id.image_gallery_layout);
        mLargeImage = (ImageView)findViewById(R.id.large_image);
        mLargeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeGallery();
            }
        });

        //progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(View.GONE);

        //hashmaps
        if(mMarkers == null) { mMarkers = new Markers(); }

        Log.d(TAG,"end of initVariables");
    }

    // TODO checks for internet connection and initialize volley
    private void initConnection() {
        Log.d(TAG,"start of initConnection");
        VolleySingleton.getInstance(this).getRequestQueue();
        Log.d(TAG,"start of initConnection");
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);

        //map position
        double latitude = Double.longBitsToDouble(prefs.getLong("latitude", 1));
        double longitude = Double.longBitsToDouble(prefs.getLong("longitude", 1));
        float bearing = prefs.getFloat("bearing", 0.0f);
        float tilt = prefs.getFloat("tilt", 0.0f);
        float zoom = prefs.getFloat("zoom", 1.0f);

        if(mLastCameraPosition == null) {
            mLastCameraPosition = new CameraPosition(new LatLng(latitude, longitude), zoom, tilt, bearing);
            Log.d(TAG, "loadPreferences mLastCameraPosition=" + mLastCameraPosition.toString());
        }

        //favorites
        String json = prefs.getString("favorites", null);
        if (json != null && json != "{}") {
            mMarkers.addFromJson(json);
            Log.d(TAG, "loadPreferences json=" + json);
        }
        Log.d(TAG, "loadPreferences mFavorites=" + mMarkers.toString());

        //display
        mMapType = prefs.getInt("maptype", GoogleMap.MAP_TYPE_NORMAL);
        mLanguageCode = prefs.getString("lang", getLanguageCode());
        bDisplayFavorites = prefs.getBoolean("disp_fav", true);
        bDisplayMarkers = prefs.getBoolean("disp_markers", true);

        Log.d(TAG, "loadPreferences prefs=" + prefs.getAll().toString());
    }

    private void initMap() {
        if (mMap == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(map);
            mapFragment.getMapAsync(this); //call this.onMapReady when ready
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mMap == null) {
            mMap = googleMap;

            mMap.setMapType(mMapType);
            mMap.setPadding(0, mTopPadding, 0, 0);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setZoomGesturesEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setRotateGesturesEnabled(true);
            mMap.getUiSettings().setTiltGesturesEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(true);
            mMap.getUiSettings().setIndoorLevelPickerEnabled(true);

            //ask permissions for location
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);

            } else {
                //Toast.makeText(this, R.string.gps_error, Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this, new String[] {
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION }, 1);
            }

            //set listeners for map interaction
            mClusterManager = new ClusterManager<>(this, mMap);
            //mClusterManager.setAlgorithm(new GridBasedAlgorithm<MarkerInfo>());
            mClusterManager.setAlgorithm(new NonHierarchicalDistanceBasedAlgorithm<MarkerInfo>());
            mClusterManager.setRenderer(new ClusterRenderer(mActivity.getApplicationContext(), mMap, mClusterManager));

            mMap.setOnPoiClickListener(new GoogleMap.OnPoiClickListener() {
                @Override
                public void onPoiClick(PointOfInterest poi) {
                    Toast.makeText(getApplicationContext(), "Clicked: " +
                                    poi.name + "\nPlace ID:" + poi.placeId +
                                    "\nLatitude:" + poi.latLng.latitude +
                                    " Longitude:" + poi.latLng.longitude,
                            Toast.LENGTH_SHORT).show();
                }
            });

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    restoreSelectedMarker();

                    //cluster or marker
                    if (marker.getTitle() == null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(),
                                (mMap.getCameraPosition().zoom + 1)), ANIMATE_CAMERA_SPEED_MS, null);
                        closeInfoWindow();
                        mSelectedMarker = null;
                    } else {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(),
                                mMap.getCameraPosition().zoom), ANIMATE_CAMERA_SPEED_MS, null);
                        String pageid = marker.getSnippet();
                        mSelectedMarker = mMarkers.get(pageid);
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker_selected));
                        showInfoWindow(mSelectedMarker.getPageID());
                    }
                    return true;
                }
            });

            mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                @Override
                public void onCameraMove() {
                    updateCamera(false);
                }
            });
            mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                @Override
                public void onCameraIdle() {
                    updateCamera(true);
                }
            });

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    closeInfoWindow();
                    restoreSelectedMarker();
                    mSelectedMarker = null;
                }
            });

            //TODO add favorites to map
            /*for (String key : mPages.keySet()) {
                MarkerWrapper marker = mPages.get(key);
                if(marker.isFavorite())
                    marker marker = mMap.addMarker(makeMapMarker(favoriteMarker));
                marker.setVisible(bDisplayFavorites);
            }*/

            // set radius circle options
            mCircle = mMap.addCircle(new CircleOptions()
                    .center(mMap.getCameraPosition().target)
                    .radius(MAX_WIKIPEDIA_API_RANGE)
                    .strokeWidth(10)
                    .strokeColor(Color.argb(128, 107, 171, 255))
                    .fillColor(Color.argb(128, 196, 222, 255))
            );

            //move camera to last known location
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(mLastCameraPosition),
                    ANIMATE_CAMERA_SPEED_MS, null);
        }
    }

    private void restoreSelectedMarker() {
        if(mSelectedMarker!=null) {
            MarkerInfo preSelected = new MarkerInfo(mSelectedMarker);
            mClusterManager.removeItem(mSelectedMarker);
            mClusterManager.addItem(preSelected);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED) {
            // We can now safely use the API we requested access to
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                // Permission was denied or request was cancelled
            }
        }
    }

    private void updateDisplayFavorites() {
        /*for(String key: mFavorites.keySet()){
            MarkerWrapper markerWrapper = mFavorites.get(key);
            markerWrapper.getMarker().setVisible(bDisplayFavorites);
        }*/
    }

    private void updateDisplayMarkers() {
        mClusterManager.clearItems();
        mMarkers.removeAllMarkers();

        if(bDisplayMarkers) {
            downloadMarkersData();
        }
    }

    //downloading data for marker in a background thread
    private void showInfoWindow(final String pageid) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    showProgressBar();
                    VolleySingleton.getInstance(mActivity).cancelPendingRequests();

                    //fetch data
                    String url = "https://" + mLanguageCode + ".wikipedia.org/w/api.php?action=query" +
                            "&prop=extracts&format=json&formatversion=2&pageids=" + pageid;

                    Log.d(TAG, "showInfoWindow url:" + url);

                    JsonObjectRequest jsObjRequest = new JsonObjectRequest
                            (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    updateInfoWindow(response);
                                    openInfoWindow();
                                    hideProgressBar();
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    hideProgressBar();
                                    Log.d(TAG, "showInfoWindow error=" + error);
                                }
                            });

                    // Add the request to the RequestQueue.
                    VolleySingleton.getInstance(mActivity).addToRequestQueue(jsObjRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void updateInfoWindow(JSONObject jsonMasterObject) {
        if (jsonMasterObject != null) {
            try {
                JSONObject errorObject = jsonMasterObject.optJSONObject("error");
                if (errorObject != null) {
                    Log.d(TAG, "updateInfoWindow errorObject = " + errorObject.toString());
                    return;
                }

                JSONObject queryObject = jsonMasterObject.optJSONObject("query");
                JSONArray pagesObject = queryObject.optJSONArray("pages");
                JSONObject pageObject = pagesObject.getJSONObject(0);

                if (pageObject == null) {
                    Log.d(TAG, "updateInfoWindow pageObject is null");
                    return;
                }

                //title
                String title = mSelectedMarker.getTitle();
                TextView tv_title = (TextView) findViewById(R.id.tv_title);
                tv_title.setText(title);
                Log.d(TAG, "Title = " + title);

                //extract
                String extract = pageObject.optString("extract");
                TextView tv_extract = (TextView) findViewById(R.id.tv_extract);
                tv_extract.setText(Html.fromHtml(extract));

                //thumbnail
                NetworkImageView niv_thumbnail = (NetworkImageView) findViewById(R.id.iv_thumbnail);
                niv_thumbnail.setImageUrl(mSelectedMarker.getThumbnailURL(), VolleySingleton.getInstance(mActivity).getImageLoader());

                //favorites
                ImageView iv_favorites = (ImageView)findViewById(R.id.update_bookmark);
                if(mSelectedMarker.isFavorite()) {
                    iv_favorites.setImageResource(R.drawable.ic_action_bookmark);
                } else {
                    iv_favorites.setImageResource(R.drawable.ic_action_bookmark_border);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateFavorites() {
        String pageid = mSelectedMarker.getPageID();
        ImageView iv_favorites = (ImageView)findViewById(R.id.update_bookmark);

        /*if (mFavorites.containsKey(pageid)){
            //in favorites -> remove
            mFavorites.remove(pageid);
            iv_favorites.setImageResource(R.drawable.favorite_off);
            if(bDisplayMarkers) {
                mClusterManager.addItem(new MarkerWrapper(mSelectedMarker));
            }

            /*Snackbar.make(findViewById(android.R.id.content), R.string.remove_from_favorites, Snackbar.LENGTH_SHORT)
                    //.setAction("Undo", mOnClickListener)
                    .setActionTextColor(Color.RED)
                    .show();*/
        /*} else {
            //not in favorites -> add to map and mFavorites
            MarkerWrapper favoriteWrapper = new MarkerWrapper(mSelectedMarker);
            favoriteWrapper.setFavorite(true);
            mFavorites.add(pageid, favoriteWrapper);
            iv_favorites.setImageResource(R.drawable.favorite_on);

            //display fav marker
            marker favoriteMarker = mMap.addMarker(makeMapMarker(favoriteWrapper));
            favoriteMarker.setVisible(bDisplayFavorites);
            favoriteWrapper.setMarker(favoriteMarker);

            //remove from cluster and mPages
            MarkerWrapper markerWrapper = mPages.get(pageid);
            mClusterManager.removeItem(markerWrapper);
            mPages.remove(pageid);
        } */

        if (mSelectedMarker.isFavorite()) {
            mSelectedMarker.setFavorite(false);
            iv_favorites.setImageResource(R.drawable.ic_action_bookmark_border);
        }
        else
        {
            mSelectedMarker.setFavorite(true);
            iv_favorites.setImageResource(R.drawable.ic_action_bookmark);
        }
        mClusterManager.cluster();
    }

    /*@NonNull
    private MarkerOptions makeMapMarker(MarkerWrapper markerWrapper) {
        return new MarkerOptions()
                .position(markerWrapper.getPosition())
                .title(markerWrapper.getTitle())
                .snippet(markerWrapper.getPageID())
                .icon(markerWrapper.getMarkerBitmap(mActivity.getApplicationContext()));
    }*/

    private void hideProgressBar() {
        mProgressBar.setVisibility(View.GONE);
    }

    private void showProgressBar() {
       mProgressBar.setVisibility(View.VISIBLE);
    }

    private void openFavoritesList() {
        /*Intent i = new Intent(this, ListActivity.class);
        i.putExtra("markers", mFavorites.getAsJSON());
        startActivity(i);*/
    }

    private void openMarkersList() {
        Intent i = new Intent(this, ListActivity.class);
        i.putExtra("markers", mMarkers);
        startActivity(i);
    }

    private void openGallery(){
        //enlarge thumbnail
        Bitmap bitmap = ((BitmapDrawable)mThumbnailImage.getDrawable()).getBitmap();
        mLargeImage.setImageBitmap(bitmap);

        if (mGalleryViewLayout.getVisibility() != View.VISIBLE) {
            //infoView.startAnimation(animShow);
            mToolbar.setVisibility(View.GONE);
            mGalleryViewLayout.setVisibility(View.VISIBLE);
        }
    }

    private void closeGallery(){
        if (mGalleryViewLayout.getVisibility() == View.VISIBLE) {
            //infoView.startAnimation(animShow);
            mLargeImage.setImageDrawable(null);
            mGalleryViewLayout.setVisibility(View.GONE);
            mToolbar.setVisibility(View.VISIBLE);
        }
    }

    private void openInfoWindow() {
        /*if (mInfoViewLayout.getVisibility() != View.VISIBLE) {
            mMap.setPadding(0, 200, 0, (mScreenHeight/3)+25);
            mInfoViewLayout.setVisibility(View.VISIBLE);
            mInfoViewLayout.startAnimation(animShow);
        }*/
        mMap.setPadding(0, mTopPadding, 0, mBottomPadding);
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void closeInfoWindow() {
        /*VolleySingleton.getInstance(mActivity).cancelPendingRequests();

        if (mInfoViewLayout.getVisibility() == View.VISIBLE) {
            mInfoViewLayout.startAnimation(animHide);
            mInfoViewLayout.setVisibility(View.GONE);
            mMap.setPadding(0, 200, 0, 0);
        }*/
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void openWebView() {
        Intent i = new Intent(this, PageActivity.class);
        i.putExtra("title", mSelectedMarker.getTitle());
        i.putExtra("lang", mSelectedMarker.getLanguage());
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_appbar, menu);

        //set check status
        menu.findItem(R.id.action_display_markers).setChecked(bDisplayMarkers);
        menu.findItem(R.id.action_display_bookmarks).setChecked(bDisplayFavorites);
        switch (mMapType) {
            case GoogleMap.MAP_TYPE_NORMAL:
                menu.findItem(R.id.action_map_type_normal).setChecked(true);
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                menu.findItem(R.id.action_map_type_satellite).setChecked(true);
                break;
            case GoogleMap.MAP_TYPE_TERRAIN:
                menu.findItem(R.id.action_map_type_terrain).setChecked(true);
                break;
            case GoogleMap.MAP_TYPE_HYBRID:
                menu.findItem(R.id.action_map_type_hybrid).setChecked(true);
                break;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                break;
            case R.id.action_bookmarks:
                openFavoritesList();
                break;
            case R.id.action_list:
                openMarkersList();
                break;
            case R.id.action_main_locale:
                //open language selection dialog
                DialogFragment LanguageFragment = new LanguageSelectionDialog();
                LanguageFragment.show(getFragmentManager(), "languages");
                break;
            case R.id.action_map_type_normal:
                item.setChecked(!item.isChecked());
                mMapType = GoogleMap.MAP_TYPE_NORMAL;
                mMap.setMapType(mMapType);
                break;
            case R.id.action_map_type_satellite:
                item.setChecked(!item.isChecked());
                mMapType = GoogleMap.MAP_TYPE_SATELLITE;
                mMap.setMapType(mMapType);
                break;
            case R.id.action_map_type_terrain:
                item.setChecked(!item.isChecked());
                mMapType = GoogleMap.MAP_TYPE_TERRAIN;
                mMap.setMapType(mMapType);
                break;
            case R.id.action_map_type_hybrid:
                item.setChecked(!item.isChecked());
                mMapType = GoogleMap.MAP_TYPE_HYBRID;
                mMap.setMapType(mMapType);
                break;
            case R.id.action_display_markers:
                item.setChecked(!item.isChecked());
                bDisplayMarkers = item.isChecked();
                updateDisplayMarkers();
                break;
            case R.id.action_display_bookmarks:
                item.setChecked(!item.isChecked());
                bDisplayFavorites = item.isChecked();
                updateDisplayFavorites();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void language_selected(int which) {
        mLanguageCode = LanguageList.getMachineReadable()[which];
        updateDisplayMarkers();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged newConfig=" + newConfig.toString());
        /*super.onConfigurationChanged(newConfig);
        mClusterManager.cluster();*/
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        VolleySingleton.getInstance(mActivity).cancelPendingRequests();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onStart () {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onRestart () {
        Log.d(TAG, "onRestart");
        super.onRestart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        savePreferences();
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    private void savePreferences() {
        if (mMap == null) {
            Log.d(TAG, "saveMapPosition mMap is null");
            return;
        }

        SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        if (settings != null) {
            SharedPreferences.Editor editor = settings.edit();

            //map parameters
            CameraPosition mapPosition = mMap.getCameraPosition();
            editor.putLong("latitude", Double.doubleToRawLongBits(mapPosition.target.latitude));
            editor.putLong("longitude", Double.doubleToRawLongBits(mapPosition.target.longitude));
            editor.putFloat("bearing", mapPosition.bearing);
            editor.putFloat("tilt", mapPosition.tilt);
            editor.putFloat("zoom", mapPosition.zoom);

            //user preferences
            editor.putInt("maptype", mMapType);
            editor.putString("lang", mLanguageCode);
            editor.putBoolean("disp_fav", bDisplayFavorites);
            editor.putBoolean("disp_markers", bDisplayMarkers);

            //favorites
            /*String json = mFavorites.getAsJSON();
            Log.d(TAG, "savePreferences json=" + json);
            editor.putString("favorites", json);*/

            editor.apply();
            Log.d(TAG,"savePreferences settings=" + settings.getAll().toString());
        }
    }

    private void updateCamera(boolean force) {
        final long snap = System.currentTimeMillis();

        if(mMap.getCameraPosition().zoom > 11) {
            //TODO google map zoom constants 10=city
            mCircle.setVisible(false);
        }
        else {
            mCircle.setCenter(mMap.getCameraPosition().target);
            mCircle.setVisible(true);
        }

        if (!force && (lastCallMs + CAMERA_MOVE_REACT_THRESHOLD_MS > snap)) {
            lastCallMs = snap;
            return;
        }

        //download markers data for this position
        mLastCameraPosition = mMap.getCameraPosition();
        lastCallMs = snap;

        if(bDisplayMarkers) {
            downloadMarkersData();
        }
    }

    //downloading data in a background thread and adding markers to map
    private void downloadMarkersData() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    showProgressBar();
                    updateCurrentLocation();
                    CameraPosition position = mMap.getCameraPosition();
                    String cameraCoordinates = position.target.latitude + "|" + position.target.longitude;
                    int radius = (int) (Math.abs(position.target.latitude - mMap.getProjection()
                            .getVisibleRegion().latLngBounds.northeast.latitude) * 110000.0d);
                    if (radius > MAX_WIKIPEDIA_API_RANGE) {
                        radius = MAX_WIKIPEDIA_API_RANGE;
                    }
                    else if (radius < MIN_WIKIPEDIA_API_RANGE) {
                        radius = MIN_WIKIPEDIA_API_RANGE;
                    }

                    String url = "https://" + mLanguageCode + ".wikipedia.org/w/api.php?action=query" +
                            "&format=json&formatversion=2&prop=coordinates|pageimages|pageterms" +
                            "&generator=geosearch&colimit=50&piprop=thumbnail&pithumbsize=100" +
                            "&pilimit=50&wbptterms=description&ggscoord=" + cameraCoordinates +
                            "&ggsradius=" + String.valueOf(radius) + "&ggslimit=" + DOWNLOAD_LIMIT;

                    if(mLastGPSLocation != null) {
                        url += "&codistancefrompoint=" + mLastGPSLocation.getLatitude() +
                                "|" + mLastGPSLocation.getLongitude() ;;
                    }

                    Log.d(TAG, "downloadData radius=" + radius + " zoom="+position.zoom+" url=" + url);

                    JsonObjectRequest jsObjRequest = new JsonObjectRequest
                            (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    clearUnseenMarkers();
                                    manipulateResult(response);
                                    mClusterManager.cluster();
                                    hideProgressBar();
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    hideProgressBar();
                                    Log.d(TAG, "downloadMarkersData onErrorResponse=" + error);
                                }
                            });

                    // Add the request to the RequestQueue.
                    VolleySingleton.getInstance(mActivity).addToRequestQueue(jsObjRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            mLastGPSLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
    }

    //loop on items and add markers to map
    public void manipulateResult(JSONObject jsonResult) {
        if (jsonResult == null)
            return;

        try {
            JSONObject errorObject = jsonResult.optJSONObject("error");
            if (errorObject != null) {
                Log.d(TAG, "manipulateResult errorObject = " + errorObject.toString());
                return;
            }

            JSONObject query = jsonResult.optJSONObject("query");
            if (query == null) {
                Log.d(TAG, "manipulateResult queryObject is null, jsonResult=" + jsonResult.toString());
                return;
            }

            JSONArray pagesArray = query.optJSONArray("pages");
            if (pagesArray == null) {
                Log.d(TAG, "manipulateResult pages is null, jsonResult=" + jsonResult.toString());
                return;
            }

            int length = pagesArray.length();
            Log.d(TAG, "manipulateResult: length="+length);
            for (int i=0; i < length; i++) {
                JSONObject page = pagesArray.getJSONObject(i);
                if(page!=null) {
                    addMarkerToMap(page);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addMarkerToMap(JSONObject jsonPage) {
        String pageid = jsonPage.optString("pageid");

        //if already on map -> discard
        if (mMarkers.containsKey(pageid)) {
            return;
        }

        try {
            String title = jsonPage.optString("title");

            JSONArray coordinates = jsonPage.optJSONArray("coordinates");
            if(coordinates == null) {
                return;
            }

            JSONObject coordinatesObject = coordinates.getJSONObject(0);
            double lat = coordinatesObject.optDouble("lat");
            double lon = coordinatesObject.optDouble("lon");
            long distance = (long) coordinatesObject.optDouble("dist");
            LatLng markerPosition = new LatLng(lat, lon);

            /*LatLngBounds bounds = this.mMap.getProjection().getVisibleRegion().latLngBounds;

            //do not add if not visible now
            if (!bounds.contains(markerPosition)) {
                Log.d(TAG, "addMarkerToMap: marker not visible " + jsonPage);
                return;
            }*/

            String thumbnailURL = "";
            JSONObject thumbnailObject = jsonPage.optJSONObject("thumbnail");
            if (thumbnailObject != null) {
                thumbnailURL = thumbnailObject.optString("source");
            }

            JSONObject terms = jsonPage.optJSONObject("terms");
            String description = "";
            if(terms != null) {
                description = terms.optJSONArray("description").optString(0);
            }

            //markerPosition = setMarkerPosition(lat, lon);
            MarkerInfo newMarker = new MarkerInfo(
                    pageid,
                    markerPosition.latitude,
                    markerPosition.longitude,
                    mLanguageCode,
                    title,
                    thumbnailURL,
                    description,
                    distance);
            mClusterManager.addItem(newMarker);
            mMarkers.add(pageid, newMarker);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void clearUnseenMarkers() {
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        Iterator<HashMap.Entry<String,MarkerInfo>> iter = mMarkers.entrySet().iterator();
        MarkerInfo markerInfo;

        while(iter.hasNext()) {
            HashMap.Entry<String,MarkerInfo> marker = iter.next();
            markerInfo = marker.getValue();
            //if out of screen and not selected, pinned or bookmarked
            if((!bounds.contains(markerInfo.getPosition())) &&
                    (mSelectedMarker==null?true:markerInfo.getPageID() != mSelectedMarker.getPageID()) &&
                    (!markerInfo.isFavorite()) &&
                    (!markerInfo.isPinned()))  {
                mClusterManager.removeItem(marker.getValue());
                iter.remove();
            }
        }
    }

    /*
    Check for overlapping position and sets a final location if needed
     */
    private LatLng setMarkerPosition(double lat, double lon){
        for(String s: mMarkers.keySet()) {
            MarkerInfo m = mMarkers.get(s);
            if(lat == m.getPosition().latitude && lon == m.getPosition().longitude) {
                //found overlapping marker -> move the marker
                lat = lat + (Math.random() -.5) / 100;
                lon = lon + (Math.random() -.5) / 100;
            }
        }
        return new LatLng(lat,lon);
    }

    @Override
    public void onBackPressed() {
        //check what views are open and close them by order
        if(mGalleryViewLayout.getVisibility() == View.VISIBLE) {
            closeGallery();
            return;
        }
        if(mBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            closeInfoWindow();
            restoreSelectedMarker();
            mSelectedMarker = null;
            return;
        }
        //close app...
        super.onBackPressed();
    }

    //https://gist.github.com/SeanZoR/9152167
    private String getLanguageCode() {
        String lang = Locale.getDefault().getLanguage();
        if (lang.equalsIgnoreCase("iw")) {
            return "he";
        } else if (lang.equalsIgnoreCase("in")) {
            return "id";
        } else if (lang.equalsIgnoreCase("ji")) {
            return "yi";
        } else {
            return lang;
        }
    }
}