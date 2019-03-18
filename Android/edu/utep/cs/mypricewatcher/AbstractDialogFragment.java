package edu.utep.cs.mypricewatcher;

import android.annotation.SuppressLint;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.utep.cs.mypricewatcher.model.Result;
import edu.utep.cs.mypricewatcher.model.WebPriceFinder;
import edu.utep.cs.mypricewatcher.model.WebStore;
import pricewatcher.model.Item;

public abstract class AbstractDialogFragment extends DialogFragment {

    /** Notify the completion of adding or editing of an item. */
    public interface Listener {
        void onCompletion(Item item);
    }

    public static class DialogViewModel extends MainViewModel {

        /** Item being added or edited. */
        public Item item;

        /** Is a network operation in progress? */
        public boolean isProgressShown;

        /** Store a newly found price by the background network operation. It
         * will be observed by this fragment regardless of config changes like
         * screen orientation (see onCreate method). */
        public final MutableLiveData<Result<Float>> result = new MutableLiveData<>();
    }

    protected Listener listener;
    protected WebPriceFinder priceFinder;
    protected DialogViewModel viewModel;
    protected Item item;
    protected List<String> groups;

    protected EditText priceEdit;
    protected EditText nameEdit;
    protected EditText urlEdit;
    protected AutoCompleteTextView groupEdit;
    protected ProgressBar progressBar;

    protected AbstractDialogFragment() {
    }

    /** Hook and callback to be invoked when the add/save button is clicked. */
    protected abstract void doneClicked(View view);

    /** Set the item to be edited. */
    public void setItem(Item item) {
        this.item = item;
    }

    /** Set the listener to be notified when the operation is completed. */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addWorkerFragment();

        viewModel = ViewModelProviders.of(getActivity()).get(DialogViewModel.class);
        if (savedInstanceState != null) { // config changes?
            item = viewModel.item;
        }

        // receive the result of a network operation (regardless of config changes).
        viewModel.result.observe(this, result -> {
            if (result != null // ignore clearing of an old value
                    && viewModel.isProgressShown) {
                    viewModel.isProgressShown = false;
                    progressBar.setVisibility(View.INVISIBLE);
                priceFound(result);
            }
        });

        priceFinder = viewModel.priceFinder(); // WebPriceFinder(getContext());
        groups = new ArrayList<>(viewModel.itemManager().groups());
    }

    protected View onCreateView(LayoutInflater inflater,
                                ViewGroup container,
                                Bundle savedInstanceState,
                                int layoutId, int doneButtonId,
                                int[] buttonColors) {
        View rootView = inflater.inflate(layoutId, container, false);
        //getDialog().setTitle("Adding/Editing");
        setCancelable(false); // prevent from cancelling by touching outside the dialog.

        int buttonColor = ContextCompat.getColor(getContext(), buttonColors[0]);
        int enabledColor = ContextCompat.getColor(getContext(), buttonColors[1]);
        int disabledColor = ContextCompat.getColor(getContext(), buttonColors[2]);

        progressBar = rootView.findViewById(R.id.progressBar);
        urlEdit = rootView.findViewById(R.id.urlEdit);
        nameEdit = rootView.findViewById(R.id.nameEdit);
        priceEdit = rootView.findViewById(R.id.priceEdit);
        priceEdit.setEnabled(false);
        priceEdit.setFocusable(false);
        groupEdit = rootView.findViewById(R.id.groupEdit);

        String url = viewModel.clearAddUrl();
        if (url != null) {
            urlEdit.setText(url);
        }

        ImageButton browseButton = rootView.findViewById(R.id.browseButton);
        browseButton.setOnClickListener(view -> showBrowseMenu(browseButton));

        ImageButton checkButton = rootView.findViewById(R.id.checkButton);
        checkButton.setOnClickListener(this::checkClicked);

        Button doneButton = rootView.findViewById(doneButtonId);
        doneButton.setOnClickListener(this::doneClicked);
        doneButton.setEnabled(false);
        doneButton.setBackgroundTintList(ColorStateList.valueOf(disabledColor));

        Button cancelButton = rootView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(view -> dismiss());

        nameEdit.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                doneButton.setEnabled(s.length() != 0 && urlEdit.length() > 0);
                doneButton.setBackgroundTintList(ColorStateList.valueOf(
                        doneButton.isEnabled() ? buttonColor : disabledColor));
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
        });

        urlEdit.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkButton.setEnabled(s.length() != 0);
                checkButton.setColorFilter(checkButton.isEnabled() ? enabledColor : disabledColor);
                doneButton.setEnabled(s.length() != 0 && nameEdit.length() > 0);
                doneButton.setBackgroundTintList(ColorStateList.valueOf(
                        doneButton.isEnabled() ? buttonColor : disabledColor));
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
        });

        groupEdit.setText(viewModel.group());
        groupEdit.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, groups));

        // make the drop-down list of the autocompletetextview align with
        // the soft keyboard's top instead of being blocked
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        return rootView;
    }

    @SuppressLint("RestrictedApi")
    protected void showBrowseMenu(ImageButton button) {
        final PopupMenu popupMenu = new PopupMenu(getActivity(), button);
        Menu menu = popupMenu.getMenu();
        int order = 1;
        if (urlEdit.length() != 0) {
            menu.add(1, -1, order++, "Current");
        }
        for (WebStore store: WebStore.values()) {
            MenuItem item = menu.add(1, store.ordinal(), order++, store.name);
            item.setIcon(store.drawableId);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            for (WebStore store : WebStore.values()) {
                if (store.ordinal() == item.getItemId()) {
                    Utils.browseUrl(getContext(), store.url);
                    return true;
                }
            }
            Utils.browseUrl(getActivity(), urlEdit.getText().toString()); // current
            return true;
        });
        MenuPopupHelper menuHelper = new MenuPopupHelper(getActivity(), (MenuBuilder) menu, button);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
        //popupMenu.show();
    }

    /** Perform a network operation to find the current value of an item. */
    private void checkClicked(View view) {
        String url = urlEdit.getText().toString();
        if (!checkEmptyValue(url, "Enter a URL first!")) {
            return;
        }
        if (workerFragment.isWorking()) {
            workerFragment.cancel();
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            viewModel.isProgressShown = false;
            toast("Canceled.");
        } else {
            viewModel.result.setValue(null); // clear old value
            progressBar.setVisibility(ProgressBar.VISIBLE);
            viewModel.isProgressShown = true;
            workerFragment.findPrice(priceFinder, url);
        }
    }

    /** Called when a new price is found by a background network operation. */
    private void priceFound(Result<Float> result) { //loat price, String title) {
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        viewModel.isProgressShown = false;
        if (result.isValue()) {
            float price = result.value;
            String title = (String) result.tag(); // piggybacked
            if (title != null && nameEdit.length() == 0) {
                nameEdit.setText(title);
            }
            priceEdit.setText(String.format("$%.02f", price));
            priceEdit.setTag(price); // piggyback
            toast(String.format("New price: $%.02f", price));
        } else {
            // error
            if (result.error != null) {
                alert(String.format("[%d] %s", -result.code, result.error));
            } else {
                toast("Unknown price!");
            }
        }
    }

    protected boolean checkEmptyValue(String value, String msg) {
        if (value == null || value.trim().length() == 0) {
            OkDialogFragment dialog = OkDialogFragment.instance(msg);
            dialog.show(getActivity().getSupportFragmentManager(), "AddOrEdit");
            return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (viewModel.isProgressShown) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // a URL from the chrome custom tab? (see the AddReceiver class)
        Intent intent = getActivity().getIntent();
        String url = intent.getStringExtra(AddReceiver.KEY_URL);
        if (url != null) {
            urlEdit.setText(url);
            intent.putExtra(AddReceiver.KEY_URL, (String) null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        viewModel.item = item;
    }

    /** Show an alert dialog. */
    protected void alert(String msg) {
        OkDialogFragment dialog = OkDialogFragment.instance(msg);
        dialog.show(getActivity().getSupportFragmentManager(), "Alert");
    }

    /** Show a toast message. */
    protected void toast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    /* Framework to access the web service in a background thread by preserving its
     * state even if there is a configuration change such as screen orientation change.
     * It is implemented using the worker fragment pattern, a fragment that retains
     * its instance during configuration changes.
     */

    /** Tag of the worker fragment. */
    private static final String TAG_WORKER_FRAGMENT = "AddEditWorkerFragment";

    /** Worker fragment pattern to preserve data and asynctask on screen orientation changes. */
    protected WorkerFragment workerFragment;

    /** Called in onCreate() to add a new worker fragment
     * with a tag TAG_WORKER_FRAGMENT if not already added. */
    private void addWorkerFragment() {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        workerFragment = (WorkerFragment) fm.findFragmentByTag(TAG_WORKER_FRAGMENT);
        if (workerFragment == null) {
            workerFragment = new WorkerFragment();
            fm.beginTransaction().add(workerFragment, TAG_WORKER_FRAGMENT).commit();
        }
    }

    /** Fragment whose instances are retained to preserver background work. */
    public static class WorkerFragment extends Fragment {

        private DialogViewModel viewModel;
        private WebPriceFinder priceFinder;

        /** Async task doing some work in background. */
        private WorkerTask worker;

        /** Called just once as an instance is retained. */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true); // retain this fragment
            viewModel = ViewModelProviders.of(getActivity()).get(DialogViewModel.class);
        }

        public void findPrice(WebPriceFinder priceFinder, String url) {
            this.priceFinder = priceFinder;
            priceFinder.setHandler(new Handler());
            worker = new WorkerTask();
            worker.execute(url);
        }

        /** Cancel the background task being performed by this fragment. */
        public void cancel() {
            if (worker != null) {
                worker.cancel(true);
                worker = null;
                priceFinder.disconnect();
            }
        }

        /** Return true if this worker is busy -- the work is in progress. */
        public boolean isWorking() {
            return worker != null;
        }

        /** Callback to receive the result from the background (async) task. */
        private void onProgress(Result<Float> result) {
            if (worker != null) { // not canceled?
                worker = null;
                viewModel.result.setValue(result); // live data observed by the dialog
                // fragment regardless of config change. The priceFound method will be
                // called.
            }
            //Log.d("Main", "=======" + result.value + "   " + result.tag);
        }

        private void onResult(Result<Float> result, boolean cancelled) {
            if (cancelled) {
                worker = null;
            }
        }

        /** Async task accessing the quiz web service. */
        private class WorkerTask extends AsyncTask<String, Result<Float>, Result<Float>> {

            @Override
            protected Result<Float> doInBackground(String... urls) {
                priceFinder.findPrice(urls[0], (price, title) -> {
                    Result<Float> result = price < 0 ? Result.error((int) price, title) : Result.value(price);
                    result.setTag(title);
                    publishProgress(result);
                });
                return null;
            }

            @Override
            protected void onProgressUpdate(Result<Float>... results) {
                super.onProgressUpdate(results);
                onProgress(results[0]);
            }

            @Override
            protected void onPostExecute(Result<Float> result) {
                super.onPostExecute(result);
                onResult(result, false);
            }

            @Override
            protected void onCancelled(Result<Float> result) {
                super.onCancelled(result);
                onResult(result, true);
            }
        }
    }
}
