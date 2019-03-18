package edu.utep.cs.mypricewatcher;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Activity to visit a given URL using a WebView. This activity is used to
 * visit the Lowe's website and specify a local store. This allows the
 * by the built-in WebView of this app to retrieve the price of an item.
 * Note that the Lowe's website doesn't give the price information unless
 * a user specify a local store.
 */
public class LowesActivity extends AppCompatActivity {

    public static final String KEY_URL = "url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lowes);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        WebView browser = findViewById(R.id.webView);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
            }
        });
        browser.loadUrl(getIntent().getStringExtra("url"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lowes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        switch (id) {
            case R.id.action_done:
                finish();
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
