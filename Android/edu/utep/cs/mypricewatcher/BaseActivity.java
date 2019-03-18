package edu.utep.cs.mypricewatcher;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import edu.utep.cs.mypricewatcher.model.Result;
import edu.utep.cs.mypricewatcher.model.WebPriceFinder;
import edu.utep.cs.mypricewatcher.settings.SettingManager;

/** A common base class of all other activity classes.
 *
 * @author Yoonsik Cheon
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected static final int REQUEST_SETTINGS = 1;
    protected SettingManager settingManager; // stateless
    private int themeId; // current theme for different text sizes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(themeId = theme()); // before super call

        super.onCreate(savedInstanceState);
        settingManager = new SettingManager(this);
        addWorkerFragment();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (themeId != theme()) { // from settings of this or called activities.
            recreate();
        }
    }

    /** Find and return the theme resource Id of this activity. May be called when this activity
     * is newly created or the settings are changed by this activity or those started by
     * this activity directly or indirectly */
    private int theme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultValue = getString(R.string.pref_text_size_default_value);
        int v = Integer.parseInt(prefs.getString(getString(R.string.pref_text_size), defaultValue));
        switch (v) {
            case 1: return R.style.SmallText;
            case 2: return R.style.MediumText;
            case 3: return R.style.LargeText;
        }
        return R.style.SmallText;
    }

    /** Called when a settings activity returns result. */
    protected void retrieveSetting() {
    }

    /** Receive new settings from SettingsActivity. */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SETTINGS) {
            if (resultCode == AppCompatActivity.RESULT_CANCELED  // by back button
                    || resultCode == AppCompatActivity.RESULT_OK) {
                retrieveSetting();
            }
        }
    }

    /** Hook for sub-subclasses to call the original onBackPressed.  */
    public final void _onBackPressed() {
        super.onBackPressed();
    }

    protected void alert(String msg) {
        OkDialogFragment dialog = OkDialogFragment.instance(msg);
        dialog.show(getSupportFragmentManager(), "Alert");
    }

    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    protected void checkNetwork() {
        if (!hasNetworkConnection()){
            enableWifi();
        }
    }

    protected boolean hasNetworkConnection() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private boolean isWifiConnected() {
        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(BaseActivity.CONNECTIVITY_SERVICE);
        // need permission: android.permission.ACCESS_NETWORK_STATE
        return manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
    }

    /** Prompt for enabling Wifi. Note that the control may not return to the caller
     * because this method may start a new activity to enable Wifi. */
    protected void enableWifi() {
        DialogFragment dialog = YesNoDialogFragment.instance(
                "No network is available. Configure Wifi or other networks?",
                () -> {
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                });
        dialog.show(getSupportFragmentManager(), "EnableWifi");
    }

    /** Check if connected to a WIFI network. This doesn't mean that
     * the device has an Internet connection, as the WIFI may not
     * 	have an Internet access. */
    private static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    /** Check if has an active Internet connection by making an HTTP
     * connection to UTEP CS website.
     */
    public boolean hasActiveInternetConnection(Context context) {
        if (isNetworkAvailable(context)) {
            HttpURLConnection urlc = null;
            try {
                urlc = (HttpURLConnection)
                        (new URL("http://www.cs.utep.edu/cheon").openConnection());
                return (urlc.getResponseCode() == 200);
            } catch (IOException e) {
                //Log.e(LOG_TAG, "Error checking Internet connection", e);
            } finally {
                if (urlc != null) {
                    urlc.disconnect();
                }
            }
        }
        return false;
    }

    //-- Notification service

    protected void createNotificationChannel(String id, String name, String description) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(id, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    protected void sendNotification(String channelId, String title, String msg) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_dollar)
                .setContentTitle(title)
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NotificationID.getID(), builder.build());
    }

    public static class NotificationID {
        private final static AtomicInteger c = new AtomicInteger(0);
        public static int getID() {
            return c.incrementAndGet();
        }
    }

    private static final String[] numberWords = { "One", "Two", "Three", "Four", "Five", "Six",
            "Seven", "Eight", "Nine"};

    protected String toWordCap(int n) {
        n -= 1;
        if (n >= 0 && n < numberWords.length) {
            return numberWords[n];
        }
        return Integer.toString(n);
    }

    protected String toWord(int n) {
        return toWordCap(n).toLowerCase();
    }

    /* Framework to access the web service in a background thread by preserving its
     * state even if there is a configuration change such as screen orientation change.
     * It is implemented using the worker fragment pattern, a fragment that retains
     * its instance during configuration changes.
     *
     * To use this fragment, do the following in a subclass:
     *
     * 1. Define a subclass-specific async task as a subclass of WorkerTask.
     *    - Override the doInBackground() method
     *
     * 2. Define a subclass-specific worker fragment as a subclass of WorkerFragment.
     *    - Introduce a new method to create an instance of worker task (of step 1)
     *      and start it.
     *
     * 3. In the activity subclass:
     *
     *    a. Override the createWorkerFragment() method to create and return
     *       an instance of the worker fragment defined in the step 2 above.
     *
     *    b. Override the workerListener() method to return a listener to be notified
     *       by the worker fragment when its work is completed.
     *
     *    c. Optionally, override the workFragment() method to change its return type, e.g.,
     *
     *       public MyWorkerFragment workFragment() {
     *          return (MyWorkerFragment) workerFragment;
     *       }
     *
     *    d. Use the following code to ask the worker fragment to start its work.
     *
     *       workFragment().myWork(x,y,z)
     *
     *       where myWork is a new method introduced in the step 2 above.
     *       To cancel the work, call workFragment().cancel().
     *       To check if work is in progress, call workFragment().isWorking().
     */

    /** Tag of the worker fragment. */
    protected static final String TAG_WORKER_FRAGMENT = "WorkerFragment";

    /** Worker fragment pattern to preserve data and asynctask on screen orientation changes. */
    protected WorkerFragment workerFragment;

    /** Called in onCreate() to add a new worker fragment
     * with a tag TAG_WORKER_FRAGMENT if not already added. */
    private void addWorkerFragment() {
        FragmentManager fm = getSupportFragmentManager();
        workerFragment = (WorkerFragment) fm.findFragmentByTag(TAG_WORKER_FRAGMENT);
        if (workerFragment == null &&
                (workerFragment = createWorkerFragment()) != null) {
            workerFragment = createWorkerFragment();
            fm.beginTransaction().add(workerFragment, TAG_WORKER_FRAGMENT).commit();
        }
    }

    /** Hook to return the current worker fragment of a subclass-specific type. */
    protected WorkerFragment workerFragment() {
        return workerFragment;
    }

    /** Hook to provide a subclass-specific work fragment.
     * By default, no worker fragment is added to this activity. */
    protected WorkerFragment createWorkerFragment() {
        return null;
    }

    /** Hook to provide a subclass-specific listener. The returned listener is
     * notified when the worker fragment completes its work.
     * by default the returned listener, does nothing upon the completion of the work. */
    protected WorkerListener workerListener() {
        return (result, cancelled) -> {}; // default: do nothing
    }

    /** To notify the completion of the work by the worker fragment. */
    public interface WorkerListener<T> {
        default void onProgress(Result<T> result) {}
        void onResult(Result<T> result, boolean cancelled);
    }

    /** Fragment whose instances are retained to preserver background work. */
    public static class WorkerFragment<P,R> extends Fragment {

        /** Async task doing some work in background. */
        protected WorkerTask<P,R> worker;

        /** Called just once as an instance is retained. */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true); // retain this fragment
        }

        /** Callback to receive the result from the background (async) task. */
        public void onResult(Result<R> result, boolean cancelled) {
            worker = null;
            if (isAdded()) {
                // use activity() to get the activity instance to which this
                // fragment is currently attached.
                activity().workerListener().onResult(result, cancelled);
            }
        }

        public void onProgress(Result<R> result) {
            if (isAdded()) {
                // use activity() to get the activity instance to which this
                // fragment is currently attached.
                activity().workerListener().onProgress(result);
            }
        }

        /** Return the activity to which this fragment is currently attached.
         * The returned activity instance may not be the same as the one that
         * created this fragment due to a configuration change, e.g., orientation change. */
        protected BaseActivity activity() {
            return (BaseActivity) super.getActivity();
        }

        /** Set the background task to be performed by this fragment. */
        public void setWorker(WorkerTask<P,R> worker) {
            this.worker = worker;
        }

        /** Attempt to cancel the background task being performed by this fragment. */
        public void cancel() {
            if (worker != null) {
                worker.cancel(true);
            }
        }

        /** Return true if this worker is busy -- the work is in progress. */
        public boolean isWorking() {
            return worker != null;
        }
    }

    /** Async task accessing the quiz web service. */
    public static abstract class WorkerTask<P,R> extends AsyncTask<P, Result<R>, Result<R>> {
        private WorkerFragment<P,R> fragment;
        private WebPriceFinder web;

        public WorkerTask<P,R> setFragmentAndService(WorkerFragment<P,R> fragment, WebPriceFinder web) {
            this.fragment = fragment;
            this.web = web;
            return this;
        }

        /** Return the web service. */
        public WebPriceFinder web() {
            return web;
        }

        /** Hook to perform a subclass-specific web operation. */
        @Override
        protected abstract Result<R> doInBackground(P... params);

        @Override
        protected void onProgressUpdate(Result<R>... results) {
            super.onProgressUpdate(results);
            fragment.onProgress(results[0]);
        }

        /** Inform the completion of the work to the listener. */
        @Override
        protected void onPostExecute(Result<R> result) {
            super.onPostExecute(result);
            fragment.onResult(result, false);
        }

        /** Inform the successful cancellation of the work to the listener. */
        @Override
        protected void onCancelled(Result<R> result) {
            fragment.onResult(result, true);
        }
    }

}
