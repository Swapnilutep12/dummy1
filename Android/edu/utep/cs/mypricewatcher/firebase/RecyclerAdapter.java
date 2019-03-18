package edu.utep.cs.mypricewatcher.firebase;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import edu.utep.cs.mypricewatcher.MainActivity;
import edu.utep.cs.mypricewatcher.R;
import edu.utep.cs.mypricewatcher.Utils;
import edu.utep.cs.mypricewatcher.model.WebStore;
import pricewatcher.model.Item;
import pricewatcher.model.ItemListModel;
import pricewatcher.model.ItemManager;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {

    private Activity activity;
    private ItemListModel items;

    public RecyclerAdapter(Activity activity, ItemManager itemManager) {
        this.activity = activity;
        this.items = new ItemListModel(itemManager, new ItemListModel.ChangeListener() {
            @Override
            public void itemInserted(int position) {
                notifyItemInserted(position);
            }

            @Override
            public void itemRemoved(int position) {
                notifyItemRemoved(position);
            }

            @Override
            public void itemChanged(int position) {
                notifyItemChanged(position);
            }

            @Override
            public void itemRangeChanged(int position, int count) {
                notifyItemRangeChanged(position, count);
            }

            @Override
            public void runOnUiThread(Runnable run) {
                activity.runOnUiThread(run);
            }
        });
    }

    /** Provide a reference to the views for each displayable element of the item. */
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView nameView;
        public TextView priceView;
        public TextView changeView;
        public TextView sinceView;
        public ImageButton viewButton;
        public ImageView storeIcon;

        /** Visual cue (check mark) to indicate whether this item is checked;
         * visible if checked; otherwise, invisible. */
        public ImageButton checkMark;

        public MyViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.nameView);
            priceView = itemView.findViewById(R.id.priceView);
            changeView = itemView.findViewById(R.id.changeView);
            sinceView = itemView.findViewById(R.id.sinceView);
            viewButton = itemView.findViewById(R.id.viewButton);
            storeIcon = itemView.findViewById(R.id.storeIcon);
            checkMark = itemView.findViewById(R.id.checkMark);
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_view, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        Item item = items.get(position);
        holder.storeIcon.setImageDrawable(storeIcon(item.url()));
        holder.storeIcon.setOnClickListener(view -> Utils.visitStore(activity, item));
        holder.nameView.setText(item.name());
        holder.priceView.setText(String.format("$%.02f", item.currentPrice()));
        holder.changeView.setTextColor(priceChangeColor(item.priceChange()));
        holder.changeView.setText(String.format("%.2f%%", item.priceChange()));
        holder.sinceView.setText(String.format("%s ($%.02f)",
                toDate(item.time()), item.initialPrice()));
        holder.viewButton.setOnClickListener(view -> Utils.browseItem(activity, item));

        // check or uncheck this item
        holder.checkMark.setVisibility(items.isChecked(item) ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(view -> {
            if (holder.checkMark.isShown()) {
                holder.checkMark.setVisibility(View.GONE);
                items.removeChecked(position);
            } else {
                holder.checkMark.setVisibility(View.VISIBLE);
                items.addChecked(position);
            }
        });

        // provide a activity menu for this item.
        holder.itemView.setOnLongClickListener(view -> {
            items.setSelected(position); // item to be manipulated
            return false;
        });
        holder.itemView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                ((MainActivity) activity).getMenuInflater().inflate(R.menu.menu_list_context, menu);
                menuIconWithText(menu);
            }
        });
    }

    /** Return the store icon of the given item url. */
    private Drawable storeIcon(String url) {
        for (WebStore store: WebStore.values()) {
            if (store.idUrl != null && url.contains(store.idUrl)) {
                return activity.getResources().getDrawable(store.drawableId, null);
            }
        }
        return activity.getResources().getDrawable(R.drawable.ic_unknown, null);
    }

    private int priceChangeColor(float change) {
        return change < 0 ? Color.rgb(17, 155, 77) :
                change == 0 ? Color.BLACK : Color.RED;
    }

    @Override
    public int getItemCount() {
        return items.itemCount();
    }

    /** Reflect the change of the given item. */
    public void changeItem(Item item) {
        int index = items.indexOf(item);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }

    /** Return all the items being displayed. */
    public List<Item> items() {
        return items.items();
    }

    /** Return the selected item or null. */
    public @Nullable Item selectedItem() {
        return items.selected();
    }

    /** Return all the items that are checked. */
    public List<Item> checked() {
        return items.checked();
    }

    /** Check all the items by showing a check mark for each item. */
    public void checkAll() {
        items.checkAll();
    }

    /** Clear all checked items. */
    public void clearChecked() {
        items.clearChecked();
    }

    /** Return true if there is any checked item. */
    public boolean hasChecked() {
        return items.hasChecked();
    }

    /** Change the store whose items are to be displayed. */
    public void setStore(String url) {
        items.setStore(url);
    }

    /** Change the group of which items are to be displayed. */
    public void setGroup(String group) {
        items.setGroup(group);
    }

    /** Change the sorter. */
    public void setSorter(ItemListModel.Sorter sorter) {
        items.setSorter(sorter);
    }

    /** Display those items whose names contains the given text. The search can be
     * further constrained by the store (url) and the item group. */
    public void search(String text, String url, String group) {
        items.search(text, url, group);
    }

    private String toDate(long time) {
        return new SimpleDateFormat("MM/dd/yyyy").format(new Date(time));
    }

    private static void menuIconWithText(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            Drawable icon = menuItem.getIcon();
            if (icon != null) {
                menuItem.setTitle(iconAndText(icon, menuItem.getTitle().toString()));
            }
        }
    }

    private static CharSequence iconAndText(Drawable img, String title) {
        img.setBounds(0, 0, img.getIntrinsicWidth(), img.getIntrinsicHeight());
        SpannableString sb = new SpannableString("    " + title);
        ImageSpan imageSpan = new ImageSpan(img, ImageSpan.ALIGN_BOTTOM);
        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }
}