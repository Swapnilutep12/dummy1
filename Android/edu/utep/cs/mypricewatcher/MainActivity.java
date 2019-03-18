package edu.utep.cs.mypricewatcher;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.webkit.WebSettings;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import edu.utep.cs.mypricewatcher.file.FileHelper;
import edu.utep.cs.mypricewatcher.firebase.FirebaseHelper;
import edu.utep.cs.mypricewatcher.firebase.RecyclerAdapter;
import edu.utep.cs.mypricewatcher.model.Result;
import edu.utep.cs.mypricewatcher.model.WebPriceFinder;
import edu.utep.cs.mypricewatcher.model.WebStore;
import edu.utep.cs.mypricewatcher.settings.MainSettingsActivity;
import edu.utep.cs.mypricewatcher.settings.SettingManager;
import edu.utep.cs.mypricewatcher.sqlite.SqliteItemManager;
import pricewatcher.model.FileItemManager;
import pricewatcher.model.FirebaseItemManager;
import pricewatcher.model.Item;
import pricewatcher.model.ItemListModel;
import pricewatcher.model.ItemManager;
import pricewatcher.model.Log;
import pricewatcher.model.WebStoreBase;

/**
 * Main screen showing the list of watched items and manipulating
 * them in various ways: adding, removing, updating prices, filtering
 * and sorting.
 *
 * The displayed items are stored and manipulated by a FilterAdapter,
 * a adapter for a recycler view, and they come from a ItemManager
 * that permanently store all the watched items. There are different
 * subclasses of the ItemManager class including FirebaseItemManager
 * and SqliteItemManager. The ItemManager class can notify
 * changes (addition, removal and update) to an observer.
 *
 * A key invariant is that there exists only one object for each item.
 * That is, item objects with different object identities are
 * considered to be different. This requirement must be met by the
 * ItemManager class so that the rest of the system doesn't have
 * to concern about a particular way of identifying items, e.g.,
 * Sqlite record id or Firebase data key.
 *
 * <pre>
 *        +---<>[MainActivity]<>----->[ItemManager]<>----+
 *        |                                              | <<notify>>
 *        +--->[FilterAdapter]----|>[ChangeListener]<----+
 * </pre>
 */

public class MainActivity extends BaseActivity {

    private static final String TAG_ADD_FRAGMENT = "AddFragment";
    private static final String TAG_EDIT_FRAGMENT = "EDITFragment";

    private MainViewModel viewModel;
    private ItemManager itemManager;
    protected SettingManager settingManager;

    private RecyclerAdapter adapter;
    private ProgressBar progressBar;
    private TextView titleView;

    static {
        Log.setLogger(Log.ANDROID);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkNetwork(); // control may or may not return here depending on user response.
        WebStoreBase.StorePriceFinder.setUserAgent(WebSettings.getDefaultUserAgent(this));

        settingManager = new SettingManager(this); // stateless
        viewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        viewModel.setPriceFinder(new WebPriceFinder(this));
        itemManager = createItemManager();
        setTitle();

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        titleView = findViewById(R.id.titleView);

        registerSelectMenu(findViewById(R.id.selectButton));
        registerFilterMenu(findViewById(R.id.filterButton));
        registerSortMenu(findViewById(R.id.sortButton));

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerAdapter(this, itemManager);
        recyclerView.setAdapter(adapter);

        itemManager.start(); // e.g., start receiving data from Firebase

        if (savedInstanceState != null) { // config changes?
            setFilter(viewModel.webStore(), viewModel.group()); // Q: unify store and group?
        }

        checkForIncomingUrl(); // any url shared by other apps?
    }

    private void setTitle() {
        boolean fb = settingManager.useFirebase();
        String title = getString(R.string.app_name) + (fb ? Character.toString((char) 0x328b) : "");
        SpannableStringBuilder cs = new SpannableStringBuilder(title);
        cs.setSpan(new SuperscriptSpan(), 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        cs.setSpan(new RelativeSizeSpan(0.5f), 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (fb) {
            int i = title.length() - 1;
            cs.setSpan(new SuperscriptSpan(), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            cs.setSpan(new RelativeSizeSpan(0.6f), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        }
        setTitle(cs);
    }

    /** Create an item manager based on the current preference setting. */
    private ItemManager createItemManager() {
        ItemManager manager = settingManager.useFirebase() ?
                FirebaseItemManager.getInstance(FirebaseHelper.getInstance())
                : (settingManager.useFile() ?
                FileItemManager.getInstance(new FileHelper(this)) :
                SqliteItemManager.getInstance(this));
        return viewModel.setItemManager(manager);
    }

    /** If this activity was launched by another app providing a url,
     * show the Add dialog. */
    private void checkForIncomingUrl() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equalsIgnoreCase(action)
                && type != null && ("text/plain".equals(type))
                && intent.hasExtra(Intent.EXTRA_TEXT)) {
            viewModel.setAddUrl(intent.getStringExtra(Intent.EXTRA_TEXT));
            toast("Received a URL.");
            showAddFragment();
        }
    }

    /** Register a filter popup menu for the given button. */
    private void registerFilterMenu(ImageButton button) {
        button.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(this, button);
            Menu menu = popupMenu.getMenu();

            // menu items for web stores
            int groupId = 1; int order = 1;
            menu.add(groupId, -1, order, getString(R.string.menu_store_all));
            groupId++;
            for (WebStore store : WebStore.values()) {
                MenuItem item = menu.add(groupId, store.ordinal(), ++order, store.name);
                item.setIcon(store.drawableId);
            }

            // submenu for item groups
            Set<String> groups = itemManager.groups();
            int itemId = 100; int subMenuId = itemId;
            if (groups.size() > 0) {
                groupId++;
                SubMenu subMenu = menu.addSubMenu(groupId, subMenuId, ++order,
                        highlightedText(getString(R.string.menu_group)));
                for (String group : groups) {
                    subMenu.add(groupId, ++itemId, ++order, group);
                }
                subMenu.add(groupId, ++itemId, ++order, ItemListModel.UNGROUPED);
            }
            int lastItemId = itemId;

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                for (WebStore store : WebStore.values()) {
                    if (store.ordinal() == id) {
                        setFilter(store, null); // show store
                        return true;
                    }
                }
                if (id == subMenuId) {
                    return true;
                }
                if (subMenuId < id && id <= lastItemId) {
                    setFilter(null, item.getTitle().toString()); // show group
                    return true;
                }
                setFilter(null, null); // show all
                return true;
            });
            showMenu(menu, button);
        });
    }

    /** Set the filter to display a subset of items, e.g., by stores or item groups. */
    private void setFilter(@Nullable WebStore store, @Nullable String group) {
        if (store != null) {
            viewModel.setWebStore(store);
            viewModel.setGroup(null);
            setTitleView(store.name);
            adapter.setStore(store.idUrl);
        } else if (group != null && !group.equals(Item.NO_GROUP)) {
            viewModel.setGroup(group);
            viewModel.setWebStore(null);
            setTitleView(group);
            adapter.setGroup(group);
        } else {
            viewModel.setWebStore(null);
            viewModel.setGroup(null);
            setTitleView(getString(R.string.watched_items_title));
            adapter.setStore(null);
        }
    }

    private CharSequence highlightedText(String title) {
        SpannableString sb = new SpannableString(title);
        sb.setSpan(new ForegroundColorSpan(Color.RED), 0, sb.length(), 0);
        return sb;
    }

    private void setTitleView(String title) {
        if (title == null || title.trim().length() == 0) {
            titleView.setText(getString(R.string.watched_items_title));
            return;
        }
        title = title.trim();
        StringBuilder sb = new StringBuilder(title);
        for (int i = 0; i < sb.length(); i++) {
            if (i == 0 || sb.charAt(i - 1) == ' ') {
                sb.setCharAt(i, Character.toUpperCase(sb.charAt(i)));
            }
        }
        titleView.setText(sb.toString());
    }

    /** Register a sort popup menu for the given button. */
    private void registerSortMenu(ImageButton button) {
        final PopupMenu popupMenu = new PopupMenu(this, button);
        Menu menu = popupMenu.getMenu();
        int order = 1; final int groupId = 1;
        for (ItemListModel.Sorter sorter : ItemListModel.Sorter.values()) {
            MenuItem item = menu.add(groupId, sorter.ordinal(), order++, sorter.name);
            item.setCheckable(true);
            if (sorter == ItemListModel.DEFAULT_SORTER) {
                item.setChecked(true);
            }
        }
        menu.setGroupCheckable(groupId, true, true);
        popupMenu.setOnMenuItemClickListener(item -> {
            for (ItemListModel.Sorter sorter : ItemListModel.Sorter.values()) {
                if (sorter.ordinal() == item.getItemId()) {
                    item.setChecked(true);
                    adapter.setSorter(sorter);
                    return true;
                }
            }
            return true;
        });
        button.setOnClickListener(view -> popupMenu.show());
    }

    /** Register a selection (checking) popup menu for the given button. */
    private void registerSelectMenu(ImageButton button) {
        button.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, button);
            popup.getMenuInflater()
                    .inflate(R.menu.menu_select, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.action_select_none:
                        adapter.clearChecked();
                        break;
                    case R.id.action_select_all:
                        adapter.checkAll();
                        break;
                }
                return true;
            });
            showMenu(popup.getMenu(), button); //popup.show();
        });
    }

    /** Force to show both the menu icon and the text. */
    @SuppressLint("RestrictedApi")
    private void showMenu(Menu menu, ImageButton button) {
        MenuPopupHelper menuHelper = new MenuPopupHelper(this, (MenuBuilder) menu, button);
        menuHelper.setForceShowIcon(true);
        menuHelper.show();
    }

    private String shortName(String name) {
        if (name.length() < 20) {
            return name;
        }
        return name.substring(0, 19) + "...";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem menuItem = menu.findItem(R.id.action_delete);
        menuItem.setTitle(menuIconWithText(menuItem.getIcon(), menuItem.getTitle().toString()));

        menuItem = menu.findItem(R.id.action_settings);
        menuItem.setTitle(menuIconWithText(menuItem.getIcon(), menuItem.getTitle().toString()));

        menuItem = menu.findItem(R.id.check_submenu);
        menuItem.setTitle(menuIconWithHighlightedText(menuItem.getIcon(), menuItem.getTitle().toString()));
        return true;
    }

    private CharSequence menuIconWithText(Drawable r, String title) {
        r.setBounds(0, 0, r.getIntrinsicWidth(), r.getIntrinsicHeight());
        SpannableString sb = new SpannableString("    " + title);
        ImageSpan imageSpan = new ImageSpan(r, ImageSpan.ALIGN_BOTTOM);
        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    private CharSequence menuIconWithHighlightedText(Drawable r, String title) {
        r.setBounds(0, 0, r.getIntrinsicWidth(), r.getIntrinsicHeight());
        SpannableString sb = new SpannableString("    " + title);
        sb.setSpan(new ForegroundColorSpan(Color.RED), 0, sb.length(), 0);
        ImageSpan imageSpan = new ImageSpan(r, ImageSpan.ALIGN_BOTTOM);
        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        switch (id) {
            case R.id.action_sync:
                if (workerFragment.isWorking()) {
                    workerFragment.cancel();
                } else {
                    findPrices();
                }
                break;

            case R.id.action_add:
                showAddFragment();
                break;

            case R.id.action_delete:
                delete();
                break;

            case R.id.action_settings:
                startSettings();
                break;

            case R.id.action_price_all:
                List<Item> items = adapter.items();
                if (items.size() > 0) {
                    findPrices(items);
                } else {
                    toast(getString(R.string.msg_no_item));
                }
                break;

            case R.id.action_price_checked:
                items = adapter.checked();
                if (items.size() > 0) {
                    findPrices(items);
                } else {
                    toast(getString(R.string.msg_selected_item));
                }
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    /** Delete all selected (checked) items if any. */
    private void delete() {
        List<Item> items = adapter.checked();
        int count = items.size();
        if (count == 0) {
            toast(getString(R.string.msg_selected_item));
            return;
        }
        String msg = count == 1 ? "One selected item. Really delete it" :
                toWordCap(count) + " selected items. Really delete all?";
        DialogFragment prompt = YesNoDialogFragment.instance(msg, () -> {
            for (Item item: items) {
                itemManager.remove(item);
            }
            toast(getString(R.string.msg_deleted));
        });
        prompt.show(getSupportFragmentManager(), "Delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        switch (id) {
            case R.id.action_price:
                findPrices(adapter.selectedItem());
                break;

            case R.id.action_browse:
                Utils.browseItem(this, adapter.selectedItem());
                break;

            case R.id.action_edit:
                showEditFragment();
                break;

            case R.id.action_delete:
                Item item = adapter.selectedItem();
                if (itemManager.remove(item) != null) {
                    toast(getString(R.string.msg_deleted));
                } else {
                    toast(getString(R.string.msg_failed_to_delete));
                }
                break;
        }
        return super.onContextItemSelected(menuItem);
    }

    private void startSettings() {
        if (workerFragment.isWorking()) {
            // price update (async task) in progress. cancel it to proceed?
            DialogFragment dialog = YesNoDialogFragment.instance(
                    "Price update in progress. Abort and proceed to Settings?", () -> {
                        workerFragment.cancel();
                        Intent intent = new Intent(this, MainSettingsActivity.class);
                        startActivityForResult(intent, REQUEST_SETTINGS);
                    });
            dialog.show(getSupportFragmentManager(), "Setting");
        } else {
            Intent intent = new Intent(this, MainSettingsActivity.class);
            startActivityForResult(intent, REQUEST_SETTINGS);
        }
    }

    @Override
    protected void retrieveSetting() {
        viewModel.priceFinder().setTimeout(settingManager.timeout());
        boolean requested = settingManager.useFirebase();
        boolean current = itemManager instanceof FirebaseItemManager;
        if (requested != current) {
            itemManager.stop();
            recreate();
        }
    }

    /**
     * Entry point for a singleTop activity. When the activity is re-launched while
     * at the top of the activity stack instead of a new instance of the activity being started,
     * this method will be called on the existing instance.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // may contain a URL (see AddReceiver)
    }

    @Override
    public void onResume() {
        super.onResume();

        // a URL from the chrome custom tab? (see the AddReceiver class)
        String url = getIntent().getStringExtra(AddReceiver.KEY_URL);
        if (url != null && !hasFragment(TAG_ADD_FRAGMENT)
                && !hasFragment(TAG_EDIT_FRAGMENT)) {
            showAddFragment();
        }

        if (workerFragment.isWorking()) { // config changes, e.g., screen orientation
            showProgress(viewModel.maxProgress(), viewModel.progress());
        }
    }

    /** Is a fragment with the given tag added to this activity? */
    private boolean hasFragment(String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        return fragment != null && fragment.isAdded();
    }

    /** Show the item add dialog. */
    private void showAddFragment() {
        AddDialogFragment dialog = new AddDialogFragment();
        //dialog.setListener(adapter::addItem); // done through itemManager
        dialog.show(getSupportFragmentManager(), TAG_ADD_FRAGMENT);
    }

    /** Show the item edit dialog. */
    private void showEditFragment() {
        EditDialogFragment dialog = new EditDialogFragment();
        dialog.setItem(adapter.selectedItem());
        //dialog.setListener(adapter::changeItem);
        dialog.show(getSupportFragmentManager(), TAG_EDIT_FRAGMENT);
    }

    /** Find and update the prices of all selected (checked) items, or
     * all items if no item is checked. */
    private void findPrices() {
        if (adapter.hasChecked()) {
            findPrices(adapter.checked());
        } else {
            List<Item> items = adapter.items();
            if (items.size() == 0) {
                toast("No item to update price!");
            } else {
                findPrices(items);
            }
        }
    }

    /** Find and update the prices of given items. */
    private void findPrices(List<Item> items) {
        findPrices(items.toArray(new Item[items.size()]));
    }

    /** Find and update the prices of given items. */
    private void findPrices(Item... items) {
        if (!hasNetworkConnection()){
            enableWifi();
            return;
        }
        if (workerFragment.isWorking()) {
            toast("Price update in progress!");
        } else {
            showProgress(items.length);
            viewModel.setSinglePriceRequest(items.length == 1);
            workerFragment().findPrices(viewModel.priceFinder(), items);
        }
    }

    /** Show the progress bar. */
    private void showProgress(int max) {
        showProgress(max, 0);
    }

    /** Show the progress bar with the given progress. */
    private void showProgress(int max, int current) {
        viewModel.setProgress(max, current);
        if (max <= 1) {
            progressBar.setIndeterminate(true);
        } else {
            progressBar.setIndeterminate(false);
            progressBar.setMax(max * 10);
            progressBar.setProgress(current > 0 ? current * 10 : 1);
        }
        progressBar.setVisibility(ProgressBar.VISIBLE);
    }

    private void unshowProgress() {
        viewModel.setProgress(0, 0);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void incProgress() {
        int progress = viewModel.incProgress();
        progressBar.setProgress(progress * 10);
    }

    /* Called when an item's price is found by the background thread. */
    private void onPriceFound(Result<Float> result) {
        incProgress();
        Item item = (Item) result.tag();
        if (result.isValue()) {
            float price = result.value;
            toast(String.format("$%.02f (%s)", price, shortName(item.name())));
            itemManager.updatePrice(item.url(), price);
        } else {
            if (viewModel.cisSinglePriceRequest() && result.error != null) {
                alert(String.format("[%d] %s", -result.code, result.error));
            } else {
                toast(String.format("%s (%s)", "Unknown price", shortName(item.name())));
            }
        }
    }

    /* Called when the background work (finding prices of all requested items) was completed. */
    private void onPriceCompleted(Result<Float> result, boolean cancelled) {
        unshowProgress();
    }

    /* Worker fragment pattern to find item prices in a background thread
     * by preserving the state upon configuration changes (e.g., orientation).
     */

    @Override
    protected WorkerFragment createWorkerFragment() {
        return new PriceWorkerFragment();
    }

    @Override
    protected PriceWorkerFragment workerFragment() {
        return (PriceWorkerFragment) workerFragment;
    }

    @Override
    protected WorkerListener workerListener() {
        return new WorkerListener() {
            @Override
            public void onProgress(Result result) {
                onPriceFound(result);
            }
            @Override
            public void onResult(Result result, boolean cancelled) {
                onPriceCompleted(result, cancelled);
            }
        };
    }

    public static class PriceWorkerFragment extends WorkerFragment<Item, Float> {
        private WebPriceFinder priceFinder;
        private int numOfItems;
        private int doneCount;

        public void findPrices(WebPriceFinder priceFinder, Item... items) {
            this.priceFinder = priceFinder;
            this.priceFinder.setHandler(new Handler()); // ui thread
            numOfItems = items.length;
            //Log.d("Main", "Total: " + numOfItems + " Last: "+ items[items.length - 1].name());
            doneCount = 0;
            worker = new PriceTask().setFragmentAndService(this, priceFinder);
            worker.execute(items);
        }

        /** Attempt to cancel the background task being performed by this fragment. */
        public void cancel() {
            super.cancel();
            priceFinder.disconnect();
            super.onResult(null, true); // notify cancel
        }

        @Override
        public void onProgress(Result<Float> result) {
            if (worker != null) { // not canceled?
                super.onProgress(result);
                doneCount++;
                //Log.d("Main", "Total: " + numOfItems + " Current: " + doneCount + " Name: " + ((Item) result.tag()).name());
                if (doneCount >= numOfItems) {
                    super.onResult(result, false); // all items done!
                }
            }
        }

        /** Callback to receive the result from the background (async) task. */
        @Override
        public void onResult(Result<Float> result, boolean cancelled) {
            if (cancelled) {
                super.onResult(result, cancelled);
            }
        }

        private class PriceTask extends WorkerTask<Item, Float> {
            @Override
            protected Result<Float> doInBackground(Item... items) {
                for (Item item: items) {
                    if (isCancelled()) {
                        return null;
                    }
                    priceFinder.findPrice(item.fullUrl(), (price, title) -> {
                        Result<Float> result = price < 0 ? Result.error((int) price, title) : Result.value(price);
                        result.setTag(item);
                        publishProgress(result);
                    });
                }
                return null;
            }
        }
    }
}
