package edu.utep.cs.mypricewatcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Receive a URL from a Chrome Tab. */
public class AddReceiver extends BroadcastReceiver {

    public static final String KEY_URL = "url";
    public static final String KEY_PRICE = "price";

    @Override
    public void onReceive(Context context, Intent intent) {
        String url = intent.getDataString();
        //Toast.makeText(activity, "URL received: " + url, Toast.LENGTH_SHORT).show();
        if (url != null) {
            Intent myIntent = new Intent(context, MainActivity.class);
            //Intent myIntent = new Intent(activity, LowesActivity.class);
            myIntent.putExtra(KEY_URL, intent.getDataString());
            myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myIntent);
        }
    }
}
