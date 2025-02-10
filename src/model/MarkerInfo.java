package com.alsolutions.mapia.model;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.alsolutions.mapia.R;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Map;

import static com.alsolutions.mapia.R.id.map;

public class MarkerInfo implements ClusterItem, Parcelable {
    private final String TAG = "MarkerInfo";

    private String mPageID;
    private LatLng mPosition;
    private String mLanguageCode;
    private String mTitle;
    private String mThumbnailURL;
    private String mDescription;
    private long mDistance; //TODO settings for distance from me or center of map
    private boolean mFavorite;
    private boolean mPinned;

    public MarkerInfo(String pageid, double lat, double lng, String lang, String title,
                      String thumbnailURL, String description, long distance) {
        mPageID = pageid;
        mPosition = new LatLng(lat,lng);
        mLanguageCode = lang;
        mTitle = title;
        mThumbnailURL = thumbnailURL;
        mDescription = description;
        mDistance = distance; //from GPS last location
        mFavorite = false;
        mPinned = false;
    }

    public MarkerInfo (MarkerInfo markerInfo) {
        mPageID = markerInfo.getPageID();
        mPosition = markerInfo.getPosition();
        mLanguageCode = markerInfo.getLanguage();
        mTitle = markerInfo.getTitle();
        mThumbnailURL = markerInfo.getThumbnailURL();
        mDescription = markerInfo.getDescription();
        mDistance = markerInfo.getDistance();
        mFavorite = markerInfo.isFavorite();
        mPinned = markerInfo.isPinned();
    }

    private MarkerInfo(Parcel in) {
        mPageID = in.readString();
        mPosition = new LatLng(in.readDouble(), in.readDouble());
        mLanguageCode = in.readString();
        mTitle = in.readString();
        mThumbnailURL = in.readString();
        mDescription = in.readString();
        mDistance = in.readLong();
        mFavorite = (in.readByte()==1);
        mPinned = (in.readByte()==1);
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mPageID);
        out.writeDouble(mPosition.latitude);
        out.writeDouble(mPosition.longitude);
        out.writeString(mLanguageCode);
        out.writeString(mTitle);
        out.writeString(mThumbnailURL);
        out.writeString(mDescription);
        out.writeLong(mDistance);
        out.writeByte((byte) (mFavorite ? 1 : 0));
        out.writeByte((byte) (mPinned ? 1 : 0));
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public MarkerInfo createFromParcel(Parcel in) {
            return new MarkerInfo(in);
        }
        public Markers[] newArray(int size) {
            return new Markers[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public LatLng getPosition() { return mPosition; }

    @Override
    public String getTitle(){ return mTitle; }

    @Override
    public String getSnippet() { return mTitle; }

    public String getPageID() { return mPageID; }
    public String getLanguage() { return mLanguageCode; }
    public String getDescription() { return mDescription; }
    public String getDistanceFormatted() {
        if(mDistance > 1000) {
            return mDistance/1000 + " km";
        }
        else {
            return mDistance + " m";
        }
    }
    public long getDistance() { return mDistance; }
    public String getThumbnailURL() { return mThumbnailURL; }
    public Boolean isFavorite(){ return mFavorite; }
    public Boolean isPinned(){ return mPinned; }
    public void setFavorite(boolean favorite){
        mFavorite=favorite;
    }
    public void setPinned(boolean pinned){ mPinned = pinned; }

    public BitmapDescriptor getMarkerBitmap(Context context) {
        BitmapDescriptor markerBitmap;

        if (isFavorite()) {
            markerBitmap = BitmapDescriptorFactory.fromResource(R.drawable.map_marker_bookmark);
        } else {
            markerBitmap = BitmapDescriptorFactory.fromResource(R.drawable.map_marker);
        }

        return markerBitmap;
    }
}