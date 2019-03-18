package edu.utep.cs.mypricewatcher.model;

import android.support.annotation.Nullable;

import pricewatcher.model.ItemManager;

public class AppModel {

    private static AppModel theInstance = new AppModel();

    private ItemManager itemManager;
    private WebPriceFinder priceFinder;
    public String group;

    /** Url shared by other apps. Stored by the main activity when it's started
     * by another app, and is passed to the Add dialog. */
    private String addUrl;

    public static AppModel getInstance() {
        return theInstance;
    }

    private AppModel() {
    }

    public ItemManager setItemManager(ItemManager itemManager) {
        return this.itemManager = itemManager;
    }

    public ItemManager itemManager() {
        return itemManager;
    }

    /** Set the URL of an item to be added to the watch list. */
    public void setAddUrl(@Nullable String url) {
        addUrl = url;
    }

    /** Clear and return the URL of the item to be added to the watch list. */
    public String clearAddUrl() {
        String copy = addUrl;
        addUrl = null;
        return copy;
    }

    public void setPriceFinder(WebPriceFinder server) {
        this.priceFinder = server;
    }

    public WebPriceFinder priceFinder() {
        return priceFinder;
    }
}
