package com.alsolutions.mapia.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static android.R.attr.key;

public class Markers implements Parcelable {
    private final String TAG = "Markers";
    private HashMap<String, MarkerInfo> mMarkersMap;

    public Markers() {
        mMarkersMap = new HashMap<>();
    }

    private Markers(Parcel in) {
        mMarkersMap = new HashMap<>();
        mMarkersMap = in.readHashMap(MarkerInfo.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap((Map)mMarkersMap);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Markers createFromParcel(Parcel in) {
            return new Markers(in);
        }

        public Markers[] newArray(int size) {
            return new Markers[size];
        }
    };

    public void add(String key, MarkerInfo marker) {
        mMarkersMap.put(key, marker);
    }

    public Boolean containsKey(String key) {
        return mMarkersMap.containsKey(key);
    }

    public MarkerInfo get(String key) {
        return mMarkersMap.get(key);
    }

    public int getSize() { return mMarkersMap.size(); }

    public Set<String> keySet() {
        return mMarkersMap.keySet();
    }

    public Collection<MarkerInfo> values() { return mMarkersMap.values(); }

    public Set entrySet() { return mMarkersMap.entrySet(); }

    public void removeMarker(String pageID) { mMarkersMap.remove(pageID); }
    public void removeAllMarkers() { mMarkersMap.clear(); }

    /*public void clear() {
        if(mMarkersMap != null) {
            for (MarkerInfo m : mMarkersMap.values()) {
                m.clearMarker();
            }
        }
    }*/

    public ArrayList<MarkerInfo> getAsList() {
        ArrayList<MarkerInfo> valueList = new ArrayList<>(mMarkersMap.values());
        return valueList;
    }

    public void addFromJson(String json) {
        if (json == null || json == "{}") {
            return;
        }
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, MarkerInfo>>(){}.getType();
        mMarkersMap = gson.fromJson(json, type);
        Log.d(TAG, "addFromJson json="+json);
    }

    public String getAsJSON() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        String json = gson.toJson(mMarkersMap, mMarkersMap.getClass());
        return json;
    }
}
