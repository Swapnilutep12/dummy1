package edu.utep.cs.mypricewatcher;

import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;

import edu.utep.cs.mypricewatcher.model.AppModel;
import edu.utep.cs.mypricewatcher.model.WebPriceFinder;
import edu.utep.cs.mypricewatcher.model.WebStore;
import pricewatcher.model.Item;
import pricewatcher.model.ItemManager;

public class MainViewModel extends ViewModel {

    protected AppModel appModel;

    public MainViewModel() {
        appModel = AppModel.getInstance();
    }

    /** Webstore whose items are being displayed. */
    private @Nullable
    WebStore webStore;

    /** True if a price update of a single item is requested. */
    private boolean singlePriceRequest;

    public ItemManager setItemManager(ItemManager itemManager) {
        return appModel.setItemManager(itemManager);
    }

    public ItemManager itemManager() {
        return appModel.itemManager();
    }

    public void setPriceFinder(WebPriceFinder server) {
        appModel.setPriceFinder(server);
    }

    public WebPriceFinder priceFinder() {
        return appModel.priceFinder();
    }

    /** Set the URL of an item to be added to the watch list. It's an url by another app,
     * is stored by the main activity when it's started by another app, and is passed
     * to the Add dialog. */
    public void setAddUrl(String url) {
        appModel.setAddUrl(url);
    }

    /** Clear and return the URL of the item to be added to the watch list. */
    public String clearAddUrl() {
        return appModel.clearAddUrl();
    }

    /** Set the Webstore whose items are being displayed. Use a null value to clear. */
    public void setWebStore(@Nullable WebStore webStore) {
        this.webStore = webStore;
    }

    /** Return the Webstore whose items are being displayed. */
    public WebStore webStore() {
        return webStore;
    }

    /** Set the group name being displayed; use null value to clear. */
    public void setGroup(@Nullable String group) {
        appModel.group = group;
    }

    /** Return the group name if an item group is being displayed; otherwise,
     * return Item.NO_GROUP. */
    public String group() {
        return appModel.group == null ? Item.NO_GROUP : appModel.group;
    }

    /** Return true if a price update of a single item is requested, and clear
     * the flag. */
    public boolean cisSinglePriceRequest() {
        boolean flag = singlePriceRequest;
        singlePriceRequest = false;
        return flag;
    }

    /** Set if a price update of a single item is requested. */
    public void setSinglePriceRequest(boolean flag) {
        singlePriceRequest = flag;
    }

    /** Maximum value of the progress bar showing the progress of price update. */
    private int maxProgress;

    /** Current value of the progress bar showing the progress of price update. */
    private int progress;

    public void setProgress(int max, int progress) {
        this.maxProgress = max;
        this.progress = progress;
    }

    public int incProgress() {
        return ++progress;
    }

    public int maxProgress() {
        return maxProgress;
    }

    public int progress() {
        return progress;
    }

}
