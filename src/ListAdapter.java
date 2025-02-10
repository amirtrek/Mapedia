package com.alsolutions.mapia;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alsolutions.mapia.model.MarkerInfo;
import com.alsolutions.mapia.model.Markers;
import com.alsolutions.mapia.model.VolleySingleton;
import com.android.volley.toolbox.NetworkImageView;

import java.util.ArrayList;

import static android.R.id.list;
import static android.support.v7.widget.AppCompatDrawableManager.get;
import static com.android.volley.VolleyLog.TAG;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ListViewHolder>
{
    private ArrayList<MarkerInfo> mMarkers;
    private ListActivity mActivity;

    public ListAdapter(Markers markers, ListActivity activity) {
        mMarkers = markers.getAsList();
        mActivity = activity;
    }

    public class ListViewHolder extends RecyclerView.ViewHolder
    {
        public TextView title;
        public TextView description;
        public TextView distance;
        public NetworkImageView thumbnail;


        public ListViewHolder(View rowView) {
            super(rowView);
            title = (TextView) rowView.findViewById(R.id.list_title);
            description = (TextView) rowView.findViewById(R.id.list_desc);
            distance = (TextView) rowView.findViewById(R.id.list_distance);
            thumbnail= (NetworkImageView) rowView.findViewById(R.id.iv_list_thumbnail);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ListAdapter.ListViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        View rowView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_row, parent, false);
        rowView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: view="+ ((TextView) view.findViewById(R.id.list_title)).getText());
                mActivity.openPageActivity(view);
            }
        });
        return new ListViewHolder(rowView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ListViewHolder holder, int position) {
        holder.title.setText(mMarkers.get(position).getTitle());
        holder.title.setTag(mMarkers.get(position).getLanguage());
        holder.description.setText(mMarkers.get(position).getDescription());
        holder.distance.setText(mMarkers.get(position).getDistanceFormatted());
        String thumbnail = mMarkers.get(position).getThumbnailURL();
        if (!thumbnail.isEmpty())
            holder.thumbnail.setImageUrl(thumbnail, VolleySingleton.getInstance(mActivity).getImageLoader());
        else
            holder.thumbnail.setImageResource(R.drawable.ic_empty_image);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mMarkers.size();
    }
}
