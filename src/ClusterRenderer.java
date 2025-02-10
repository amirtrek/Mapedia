package com.alsolutions.mapia;

import android.content.Context;
import android.util.Log;

import com.alsolutions.mapia.model.MarkerInfo;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

class ClusterRenderer extends DefaultClusterRenderer<MarkerInfo> {
    private final String TAG = "ClusterRenderer";
    private Context mContext;
    private static final int minClusterSize = 4;

    public ClusterRenderer(Context context, GoogleMap map,
                           ClusterManager<MarkerInfo> clusterManager) {
        super(context, map, clusterManager);
        mContext = context;
    }

    @Override
    protected void onBeforeClusterItemRendered(MarkerInfo markerWrapper, MarkerOptions markerOptions) {
        markerOptions.title(markerWrapper.getTitle());
        markerOptions.snippet(markerWrapper.getPageID());
        markerOptions.icon(markerWrapper.getMarkerBitmap(mContext));
        markerOptions.draggable(false);
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<MarkerInfo> cluster) {
        return cluster.getSize() > minClusterSize;
    }

    @Override
    protected void onClusterItemRendered(MarkerInfo markerInfo, Marker marker) {
        super.onClusterItemRendered(markerInfo, marker);
    }
}