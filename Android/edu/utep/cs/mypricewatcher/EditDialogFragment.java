package edu.utep.cs.mypricewatcher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import pricewatcher.model.ItemManager;

import static android.webkit.URLUtil.isValidUrl;

public class EditDialogFragment extends AbstractDialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int[] buttonColors = {
                R.color.colorEditButton,
                android.R.color.holo_orange_light, // enabled
                R.color.colorDisabled}; // disabled
        View rootView = onCreateView(inflater, container, savedInstanceState,
                R.layout.fragment_edit_dialog, R.id.saveButton, buttonColors);
        urlEdit.setText(item.url());
        nameEdit.setText(item.name());
        priceEdit.setText(String.format("$%.02f", item.initialPrice()));
        groupEdit.setText(item.group());
        return rootView;
    }

    /** Called when the save button is clicked. */
    protected void doneClicked(View view) {
        String name = nameEdit.getText().toString();
        if (!checkEmptyValue(name, "Enter a name!")) {
            return;
        }
        String url = urlEdit.getText().toString();
        if (!checkEmptyValue(url, "Enter a URL!")) {
            return;
        }
        if (!isValidUrl(url)) {
            OkDialogFragment dialog = OkDialogFragment.instance("Enter a valid URL!");
            dialog.show(getActivity().getSupportFragmentManager(), "Add");
            return;
        }

        String group = groupEdit.getText().toString();
        if (url.equals(item.url())) {
            if (name.equals(item.name()) && group.equals(item.group())) {
                toast("No change!");
            } else {
                save(name, url, group);
            }
        } else {
            if (viewModel.itemManager().contains(url)) {
                toast("URL already exists!");
            } else {
                save(name, url, group);
            }
        }
    }

    private void save(String name, String url, String group) {
        ItemManager itemManager = viewModel.itemManager();
        float price = priceEdit.getTag() == null ? item.currentPrice() : (float) priceEdit.getTag();
        item.setName(name);
        item.setUrl(url);
        item.setPrice(price);
        item.setGroup(group);
        if (itemManager.change(item) != null) {
            toast("Saved.");
            dismiss();
            if (listener != null) {
                listener.onCompletion(item);
            }
        } else {
            toast("Failed to save!");
        }
    }
}
