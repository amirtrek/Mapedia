package com.alsolutions.mapia;

import android.location.Location;

import com.alsolutions.mapia.model.MarkerInfo;
import com.alsolutions.mapia.model.Markers;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.clustering.ClusterManager;

/**
 * Created by amir on 16/3/17.
 */

class MapData {
    private GoogleMap mMap;
    private ClusterManager<MarkerInfo> mClusterManager;
    private Markers mMarkers;
    private MarkerInfo mSelectedMarker;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastGPSLocation;
}
