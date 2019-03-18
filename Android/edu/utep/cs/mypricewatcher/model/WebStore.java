package edu.utep.cs.mypricewatcher.model;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.Html;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.ConcurrentLinkedDeque;

import edu.utep.cs.mypricewatcher.LowesActivity;
import edu.utep.cs.mypricewatcher.MainActivity;
import edu.utep.cs.mypricewatcher.R;
import edu.utep.cs.mypricewatcher.YesNoDialogFragment;
import pricewatcher.model.WebPriceFinder;
import pricewatcher.model.WebStoreBase;

import static pricewatcher.model.WebPriceFinder.ERROR_LOWES_UNKNOWN_STORE;
import static pricewatcher.model.WebPriceFinder.ERROR_NETWORK;
import static pricewatcher.model.WebPriceFinder.ERROR_PRICE_DETECTION;
import static pricewatcher.model.WebPriceFinder.PRICE_NOT_FOUND;

public enum WebStore {

    AMAZON(WebStoreBase.AMAZON, R.drawable.ic_amazon),

    BEST_BUY(WebStoreBase.BEST_BUY, R.drawable.ic_bestbuy),

    EBAY(WebStoreBase.EBAY, R.drawable.ic_ebay),

    HOME_DEPOT(WebStoreBase.HOME_DEPOT, R.drawable.ic_homedepot),

    KHOLS(WebStoreBase.KHOLS, R.drawable.ic_kohls),

    LOWES("Lowes", "www.lowes.com", "lowes.com", R.drawable.ic_lowes, new LowesPriceFinder());

    public final String name;
    public final String url;
    public final String idUrl;
    public final int drawableId;
    public final WebStoreBase.StorePriceFinder priceFinder;

    WebStore(WebStoreBase base, int drawableId) {
        this.drawableId = drawableId;
        name = base.name;
        url = base.url;
        idUrl = base.idUrl;
        priceFinder = base.priceFinder;
    }

    WebStore(String name, String url, String idUrl, int drawableId, WebStoreBase.StorePriceFinder priceFinder) {
        this.name = name;
        this.url = url;
        this.idUrl = idUrl;
        this.drawableId = drawableId;
        this.priceFinder = priceFinder;
    }

    static {
        WebStoreBase.StorePriceFinder.setHtmlEscaper(text ->
                Html.fromHtml(text, 0).toString());
    }

    public String fullUrl() {
        return "http://" + url;
    }

    /** Find the price of a Lowe's item using a WebView to process JavaScripts. */
    public static class LowesPriceFinder extends WebStoreBase.StorePriceFinder {

        /** Request consisting of an item URL and a listener. */
        private static class Request {
            public final String url;
            public final WebPriceFinder.Listener listener;
            public Request(String url, WebPriceFinder.Listener listener) {
                this.url = url;
                this.listener = listener;
            }
        }

        /** Queue of requests. A single web view is used to process requests
         * sequentially one at a time. */
        private ConcurrentLinkedDeque<Request> requests = new ConcurrentLinkedDeque<>();

        /** Used to process all incoming price requests. */
        private WebView webView;

        /** True if there is no active thread processing requests. E.g.,
         * when there is no pending request in the request queue. */
        private boolean isIdle = true;

        /** Handler of the UI thread to manipulate a web view. */
        private Handler handler;

        private Context context;

        /** Set the activity and the handler. This method must be called before calling
         *  the findPrice() method. */
        public void setContext(Context context, Handler handler) {
            this.context = context;
            this.handler = handler;
        }

        @Override
        protected float query(String urlString) {
            throw new UnsupportedOperationException();
        }

        public synchronized void findPrice(String url, WebPriceFinder.Listener listener) {
            requests.add(new Request(url, listener));
            if (isIdle) {
                isIdle = false;
                handler.post(() -> handleNextRequest());
            }
        }

        /** To be run on the handler (UI) thread. */
        private synchronized void handleNextRequest() {
            if (requests.isEmpty()) {
                isIdle = true;
                webView = null; // not reusable when the activity is changed
            } else {
                Request request = requests.poll();
                if (webView == null) {
                    webView = createWebView(request.listener);
                }
                webView.loadUrl(request.url);
                //Log.d("Main", "========== " + request.url);
            };
        }

        @Override
        public void disconnect() {
            requests.clear();
            if (handler != null) { // may be called even when not active
                handler.post(() -> {
                    if (webView != null) {
                        webView.stopLoading();
                        webView.destroy();
                        webView = null;
                    }
                });
            }
        }

        private void postResult(WebPriceFinder.Listener listener, float price, String title) {
            if (listener != null) {
                handler.post(() -> listener.priceFound(price, title));
            }
            handler.post(() -> handleNextRequest());
        }

        private WebView createWebView(WebPriceFinder.Listener listener) {
            WebView webView = new WebView(context);
            WebSettings webSettings= webView.getSettings();
            webSettings.setJavaScriptEnabled(true);

            webView.setWebChromeClient(new WebChromeClient(){
                public void onReceivedTitle (WebView view, String title) {
                    LowesPriceFinder.this.title = title;
                }
            });

            webView.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    //Log.d("Main", "+++++++++++ " + url);
                    /* // can be done with a webchromeclient
                    String code = "var x = document.getElementsByTagName('title'); return x.length > 0 ? x[0].innerHTML : '';";
                    view.evaluateJavascript("(function() {" + code + "})();", json -> {
                        // JSON Value, JSON Object or JSON Array depending on what you return.
                        title = parseJson(json);
                        title = title.length() == 0 ? null : title;
                    });
                    */

                    String body = "var x = document.getElementsByClassName('store-name'); if (x.length == 0) { return 'UNKNOWN_STORE'; } x = document.getElementsByClassName('art-pd-price'); if (x.length == 0) { x = document.getElementsByClassName('art-pd-contractPricing'); } return x.length > 0 ? x[0].innerHTML : '0';";
                    view.evaluateJavascript("(function() {" + body + "})();", json -> {
                        // JSON Value, JSON Object or JSON Array depending on what you return.
                        postResult(listener, parseLowesPrice(json), LowesPriceFinder.this.title);
                    });
                }

                public void onReceivedError (WebView view, WebResourceRequest request, WebResourceError error) {
                    //Log.d("Main", "------------- " + request.getUrl());
                    //Log.d("Main", "------------- " + error.getErrorCode() + " " + error.getDescription());
                    postResult(listener, PRICE_NOT_FOUND, null);
                }
            });
            return webView;
        }

        private float parseLowesPrice(String json) {
            String html = parseJson(json);
            if (html != null) {
                if ("UNKNOWN_STORE".equals(html)) {
                    String msg = "Need to specify your Lowe's store. Visit the Lowe's website to find your local store now?";
                    YesNoDialogFragment dialog = YesNoDialogFragment.instance(msg, () -> {
                        Intent intent = new Intent(context, LowesActivity.class);
                        intent.putExtra(LowesActivity.KEY_URL, WebStore.LOWES.fullUrl() + "/store");
                        context.startActivity(intent);
                    });
                    dialog.show(((MainActivity) context).getSupportFragmentManager(), "Find store");
                    error = "Lowes: Unknown local store";
                    return ERROR_LOWES_UNKNOWN_STORE;
                }
                try {
                    int i = html.lastIndexOf(">");
                    // string: ">1,499.00" => float: 1499.0
                    return Float.parseFloat(html.substring(i + 1).replace(",", ""));
                } catch (IndexOutOfBoundsException | NumberFormatException e) {
                    error = "Lowes price detection: failed";
                    Log.e("WebPriceFinder", "IndexOutOfBoundException or NumberFormatException", e);
                }
                return ERROR_PRICE_DETECTION;
            } else {
                return ERROR_NETWORK;
            }
        }

        private String parseJson(String json) {
            JsonReader reader = new JsonReader(new StringReader(json));
            // must set lenient to parse single values
            reader.setLenient(true);
            try {
                if (reader.peek() != JsonToken.NULL) {
                    if (reader.peek() == JsonToken.STRING) {
                        return reader.nextString();
                    }
                }
            } catch (IOException e) {
                error = "Network error: " + e.getMessage();
                Log.e("WebPriceFinder", "WebPriceFinder: IOException", e);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            return null;
        }
    }
}
