package edu.utep.cs.mypricewatcher;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import edu.utep.cs.mypricewatcher.model.WebStore;
import pricewatcher.model.Item;

public class Utils {

    public static void browseUrl(Context context, String url) {
        if (!url.toLowerCase().startsWith("http")) {
            url = String.format("http://%s", url);
        }
        launchUrl(context, url, true);
    }

    public static void browseItem(Context context, Item item) {
        launchUrl(context, item.fullUrl(), true);
    }

    public static void visitStore(Context context, Item item) {
        String url = item.fullUrl();
        try {
            int toIndex = url.indexOf("/", url.indexOf("//") + 2);
            url = url.substring(0, toIndex);
        } catch (StringIndexOutOfBoundsException e) {
            toast(context, "Invalid URL: " + url);
            return;
        }
        launchUrl(context, url, true);
    }

    /**
     * Launch the given URL with the Chrome custom tab and optionally set
     * an action item to broadcast the URL of the page being viewed.
     */
    private static void launchUrl(Context context, String url, boolean addAction) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary));
        if (addAction) {
            Intent actionIntent = new Intent(context, AddReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                    actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            String addLabel = context.getString(R.string.action_add);
            Bitmap icon = Utils.getBitmap(context, R.drawable.ic_add_plus);
            builder.setActionButton(icon, addLabel, pendingIntent);
        }
        Bitmap closeButton = Utils.getBitmap(context, R.drawable.ic_arrow_back);
        builder.setCloseButtonIcon(closeButton);
        builder.setStartAnimations(context, R.anim.tab_in, R.anim.main_out);
        builder.setExitAnimations(context, R.anim.main_in, R.anim.tab_out);
        CustomTabsIntent customTabsIntent = builder.build();
        try {
            customTabsIntent.launchUrl(context, Uri.parse(url));
        } catch (ActivityNotFoundException e) {
            toast(context, "Invalid URL: " + url);
        }
    }

    public static Bitmap getBitmap(Context context, int drawableRes) {
        Drawable drawable = context.getResources().getDrawable(drawableRes, null);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static String imageUrl(String url) {
        String imageUrl = null;
        for (WebStore store: WebStore.values()) {
            if (store.idUrl != null && url.contains(store.idUrl)) {
                return getURLForResource(store.drawableId);
            }
        }
        return getURLForResource(R.drawable.ic_unknown);
    }

    private static String getURLForResource (int resourceId) {
        return Uri.parse("android.resource://"+R.class.getPackage().getName()+"/" +resourceId).toString();
    }

    /** Show a toast message. */
    public static void toast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
