package edu.utep.cs.mypricewatcher.model;

import android.content.Context;
import android.os.Handler;

public class WebPriceFinder extends pricewatcher.model.WebPriceFinder {

    private final Context context;
    private Handler handler; // handler of UI thread

    public WebPriceFinder(Context context) {
        this.context = context;
    }

    /** Set the handler of the UI thread, as needed by Lowes price finder that uses
     * a WebView. */
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void findPrice(String url, Listener listener) {
        url = url.toLowerCase();
        for (WebStore store: WebStore.values()) {
            if (store == WebStore.LOWES && (url.contains(store.idUrl))) {
                ((WebStore.LowesPriceFinder) store.priceFinder).setContext(context, handler);
                priceFinder = store.priceFinder;
                priceFinder.setTimeout(timeout); // TODO: not used
                priceFinder.findPrice(url, listener);
                return;
            } else if (url.contains(store.idUrl)) {
                priceFinder = store.priceFinder;
                priceFinder.setTimeout(timeout);
                priceFinder.findPrice(url, listener);
                return;
            }
        }
        listener.priceFound(ERROR_UNKNOWN_STORE, "Unknown store: " + url);
    }

    /** Close the current connection to the priceFinder if exists. */
    @Override
    public void disconnect() {
        super.disconnect();
        if (priceFinder != WebStore.LOWES.priceFinder) { // async processing
            WebStore.LOWES.priceFinder.disconnect();
        }
    }
}
