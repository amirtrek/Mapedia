package com.alsolutions.mapia;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.alsolutions.mapia.model.VolleySingleton;

public class PageActivity extends AppCompatActivity {
    private final String TAG = "PageActivity";

    private String mTitle;
    private String mLang;
    private WebView mWebView;
    private Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);

        mActivity = this;
        Toolbar toolbar = (Toolbar) findViewById(R.id.activity_page_toolbar);
        setSupportActionBar(toolbar);

        mTitle = getIntent().getStringExtra("title");
        mLang = getIntent().getStringExtra("lang");
        mWebView = (WebView) findViewById(R.id.fullPageWebView);

        initWebView();

        this.runOnUiThread(new Runnable() {
            public void run() {
                VolleySingleton.getInstance(mActivity).cancelPendingRequests();

                //fetch data
                //TODO query with API and reformat
                String url = "https://" + mLang + ".m.wikipedia.org/wiki/" + mTitle;
                Log.d(TAG, "openWebView url:" + url);
                mWebView.loadUrl(url); //TODO show progress of loading
            }
        });
    }

    private void initWebView() {
        WebSettings settings = mWebView.getSettings();
        //settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        //webView.getSettings().setTextZoom(50); //TODO read from settings
        //settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setLoadsImagesAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);

        mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(true);

        //handling internal urls
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG,"webView.setWebViewClient shouldOverrideUrlLoading url="+url);
                view.loadUrl(url);
                return true;
            }
        });
    }
}
