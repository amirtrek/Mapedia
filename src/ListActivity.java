package com.alsolutions.mapia;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.alsolutions.mapia.model.Markers;
import com.google.android.gms.maps.GoogleMap;

public class ListActivity extends AppCompatActivity {
    private String TAG = "ListActivity";

    private Markers mMarkersMap;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.activity_list_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mMarkersMap = getIntent().getParcelableExtra("markers");

        mRecyclerView = (RecyclerView) findViewById(R.id.list_RecyclerView);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new ListAdapter(mMarkersMap, this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void openPageActivity(View view) {
        String title = ((TextView) view.findViewById(R.id.list_title)).getText().toString();
        String lang = ((TextView) view.findViewById(R.id.list_title)).getTag().toString();
        Intent i = new Intent(this, PageActivity.class);
        i.putExtra("title", title);
        i.putExtra("lang", lang);
        startActivity(i);

    }
}